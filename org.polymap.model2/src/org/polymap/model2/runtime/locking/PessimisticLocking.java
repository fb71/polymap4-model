/* 
 * polymap.org
 * Copyright (C) 2012-2016, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.runtime.locking;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import java.lang.ref.WeakReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.MapMaker;

import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyConcern;
import org.polymap.model2.PropertyConcernBase;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * Provides base abstractions of pessimistic locking of {@link Entity}s accessed from
 * different {@link UnitOfWork} (not Thread!) instances.
 * <p>
 * <b>Beware</b>: Not thoroughly tested yet. Implementation currently uses polling
 * and {@link WeakReference} to get informed about the end of an {@link UnitOfWork}.
 * <p>
 * Implementation uses one global map for all locks. This map is filled with
 * {@link EntityLock} instances from all {@link EntityRepository} instances of the
 * lifetime of the JVM. Entries are never evicted from this global map.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class PessimisticLocking
        extends PropertyConcernBase
        implements PropertyConcern, ManyAssociation/*, Association*/ {

    private static final Log log = LogFactory.getLog( PessimisticLocking.class );

    private static ConcurrentMap<EntityKey,EntityLock>  locks = new MapMaker().concurrencyLevel( 4 ).initialCapacity( 256 ).makeMap();
    
    protected enum AccessMode {
        READ, WRITE
    }

    /**
     * 
     *
     * @param uow
     */
    public static void notifyClosed( UnitOfWork uow ) {
        locks.forEach( (key, lock) -> lock.checkRelease( uow ) );
    }
    
    
    // instance *******************************************
    
    @Override
    public Object get() {
        lock( AccessMode.READ );
        return ((Property)delegate).get();
    }
    
    @Override
    public Iterator iterator() {
        lock( AccessMode.READ );
        return ((ManyAssociation)delegate).iterator();
    }
    
    @Override
    public Object createValue( ValueInitializer initializer ) {
        lock( AccessMode.WRITE );
        return ((Property)delegate).createValue( initializer );
    }
    
    @Override
    public void set( Object value ) {
        lock( AccessMode.WRITE );
        ((Property)delegate).set( value );
    }
    
    @Override
    public boolean add( Object e ) {
       lock( AccessMode.WRITE );
       return ((ManyAssociation)delegate).add( e );
    }
    
    @Override
    public boolean addAll( Collection c ) {
       lock( AccessMode.WRITE );
       return ((ManyAssociation)delegate).addAll( c );
    }

    @Override
    public boolean remove( Object e ) {
       lock( AccessMode.WRITE );
       return ((ManyAssociation)delegate).remove( e );
    }

    @Override
    public boolean removeAll( Collection c ) {
       lock( AccessMode.WRITE );
       return ((ManyAssociation)delegate).removeAll( c );
    }

    
    protected void lock( AccessMode accessMode ) {
        UnitOfWork uow = context.getUnitOfWork();
        Entity entity = context.getEntity();
        EntityKey key = new EntityKey( entity );
        
        EntityLock entityLock = locks.computeIfAbsent( key, k -> newLock( k, entity ) );
        entityLock.aquire( uow, accessMode );
    }

    
    protected abstract EntityLock newLock( EntityKey key, Entity entity );

    
    /**
     * 
     */
    protected abstract class EntityLock {

        /**
         * Deadlock detection: the {@link UnitOfWork} we are currently waiting on.
         */
        private volatile UnitOfWork         waitingOn;

        public abstract void aquire( UnitOfWork uow, AccessMode accessMode );
        
        public abstract UnitOfWork aquiredBy();
        
        public abstract void checkRelease( UnitOfWork uow );
        
        
        /**
         * 
         *
         * @param condition
         * @param mode
         */
        protected void await( Supplier<Boolean> condition, AccessMode mode ) {
            try {
                boolean firstLoop = true;
                while (!condition.get()) {
                    log.warn( Thread.currentThread().getName() + ": await lock: " + mode + " on: " + context.getEntity().id() );

                    if (!firstLoop) {
                        waitingOn = aquiredBy();
                        //checkDeadlock();
                    }
                    firstLoop = false;

                    try { wait( 100 ); } catch (InterruptedException e) { }

                    cleanStaleHolders();
                }
            }
            finally {
                waitingOn = null;
            }
        }
        
        
        /**
         * Check for circle in the locking graph.
         * <p/>
         * XXX This implementation assumes that the lock status of the
         * {@link UnitOfWork} instances do not change during runtime of the method.
         * This is not always true. This may cause a {@link DeadlockException} to be thrown
         * we no deadlock (?).
         * 
         * @throws DeadlockException
         */
        protected void checkDeadlock() throws DeadlockException {
            log.warn( "    checking deadlock, start: " + context.getEntity().id() );
            Set<UnitOfWork> seen = new HashSet();
            
            Deque<EntityLock> toBeChecked = new ArrayDeque();
            toBeChecked.push( this );

            for (EntityLock next=toBeChecked.poll(); next!=null; next=toBeChecked.poll()) {
                UnitOfWork blocker = next.aquiredBy();
                if (blocker != null) {
                    // check / add
                    if (!seen.add( blocker )) {
                        throw new DeadlockException( "Deadlock detected" + 
                                seen.stream().map( l -> context.getEntity().id() ).reduce( "", (c,n) -> (c + " -> " + n) ) );
                    }
                    // check all locks which are waiting for the blocker
                    for (EntityLock l : locks.values()) {
                        if (/*l != next &&*/ l.waitingOn == blocker) {
                            toBeChecked.push( l );
                        }
                    }
                }
            }
        }
        
        protected void cleanStaleHolders() {
        }
    }

    
    /**
     * 
     */
    protected class EntityKey {
        
        private String      key; 
    
        public EntityKey( Entity entity ) {
            key = entity.getClass().getName() + entity.id().toString();
        }
    
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    
        @Override
        public boolean equals( Object obj ) {
            return key.equals( ((EntityKey)obj).key );
        }        
    }

}
