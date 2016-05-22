/* 
 * polymap.org
 * Copyright (C) 2012-2014, Falko Bräutigam. All rights reserved.
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

import static org.polymap.model2.runtime.locking.PessimisticLocking.AccessMode.READ;
import static org.polymap.model2.runtime.locking.PessimisticLocking.AccessMode.WRITE;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Entity;
import org.polymap.model2.engine.cache.ConcurrentReferenceHashMap;
import org.polymap.model2.engine.cache.ConcurrentReferenceHashMap.ReferenceType;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * Implements Multiple-Readers/One-Writer {@link PessimisticLocking}.
 * <p/>
 * There is <b>no deadlock detection</b>!
 * <p/>
 * Not thoroughly tested yet. See {@link PessimisticLocking} for general limitations.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class MrowPessimisticLocking
        extends PessimisticLocking {

    private static final Log log = LogFactory.getLog( MrowPessimisticLocking.class );

    
    @Override
    protected EntityLock newLock( EntityKey key, Entity entity ) {
        return new MrowEntityLock();
    }


    /**
     * 
     */
    protected class MrowEntityLock
            extends EntityLock {
        
        private ConcurrentReferenceHashMap<Integer,UnitOfWork> 
                                        readers = new ConcurrentReferenceHashMap( 8, 0.75f, 2, ReferenceType.STRONG, ReferenceType.WEAK, null );

        private Reference<UnitOfWork>   writer;        

        @Override
        public void aquire( UnitOfWork uow, AccessMode accessMode ) {
            // read lock
            if (!readers.containsKey( uow.hashCode() )) {
                synchronized (this) {
                    await( () -> writer == null || writer.get() == null || !writer.get().isOpen(), READ );
                    readers.put( uow.hashCode(), uow );
                }
            }   
            // write lock
            if (accessMode == WRITE) {
                // do we have to lock? -> avoid synchronize
                if (writer != null && writer.get() == uow) {
                    return;
                }
                else {
                    synchronized (this) {
                        await( () -> readers.size() == 1 /*|| (writer != null && writer.get() != uow)*/, WRITE );
                        writer = new WeakReference( uow );
                    }
                }
            }
        }

        @Override
        public UnitOfWork aquiredBy() {
            return null;
        }

        @Override
        public void checkRelease( UnitOfWork uow ) {
            if (writer != null && writer.get() == uow
                    || readers.remove( uow.hashCode() ) != null) {
                synchronized (this) {
                    writer = null;
                    notifyAll();
                }
            }
        }

        @Override
        protected void cleanStaleHolders() {
            // writer gets checked by condition
            readers.values().stream().filter( r -> !r.isOpen() ).forEach( r -> readers.remove( r.hashCode() ) );
        }
        
    }

}
