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
package org.polymap.model2.engine;

import static java.util.Collections.singletonList;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.CREATED;
import static org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus.MODIFIED;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import java.io.IOException;

import javax.cache.Cache.Entry;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

import org.polymap.model2.Entity;
import org.polymap.model2.engine.cache.LoadingCache.Loader;
import org.polymap.model2.query.Query;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.model2.runtime.Lifecycle.State;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.CloneCompositeStateSupport;
import org.polymap.model2.store.CompositeState;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class UnitOfWorkNested
        extends UnitOfWorkImpl {

    /** The parent UnitOfWork in case of nested instances, or null for the root UnitOfWork. */
    protected UnitOfWorkImpl        parent;

    
    protected UnitOfWorkNested( EntityRepositoryImpl repo, CloneCompositeStateSupport storeUow, UnitOfWorkImpl parent ) {
        super( repo, storeUow );
        this.parent = parent;
        assert parent != null : "parent must not be null.";
    }
    
    
    CloneCompositeStateSupport storeUow() {
        return (CloneCompositeStateSupport)storeUow;    
    }
    
    
    @Override
    public <T extends Entity> T entity( final Class<T> entityClass, final Object id ) {
        assert entityClass != null;
        assert id != null;
        checkOpen();
        T result = (T)loaded.get( id, new Loader<Object,Entity>() {
            public Entity load( Object key ) throws RuntimeException {
                // just clone the entire Entity and its state; copy-on-write would probably
                // be faster and less memory consuming but also would introduce a lot more complexity;
                // maybe I will later investigate a global copy-on-write cache for Entities
                T parentEntity = parent.entity( entityClass, id );
                if (parentEntity == null) {
                    return null;
                }
                else {
                    CompositeState parentState = repo.contextOfEntity( parentEntity ).getState();
                    CompositeState state = storeUow().cloneEntityState( parentState );
                    return repo.buildEntity( state, entityClass, UnitOfWorkNested.this );
                }
            }
        });
        return result != null && result.status() != EntityStatus.REMOVED ? result : null;
    }


    @Override
    public <T extends Entity> T entityForState( final Class<T> entityClass, Object state ) {
        throw new RuntimeException( "not yet implemented." );
//        checkOpen();
//        
//        final CompositeState compositeState = storeUow.adoptEntityState( state, entityClass );
//        final Object id = compositeState.id();
//        
//        return (T)loaded.get( id, new EntityCacheLoader() {
//            public Entity load( Object key ) throws RuntimeException {
//                return repo.buildEntity( compositeState, entityClass, UnitOfWorkNested.this );
//            }
//        });
    }

    
    @Override
    public void removeEntity( Entity entity ) {
        assert entity != null : "entity must not be null.";
        checkOpen();
        repo.contextOfEntity( entity ).raiseStatus( EntityStatus.REMOVED );
    }


    @Override
    public <T extends Entity> Query<T> query( final Class<T> entityClass ) {
        return new Query<T>( entityClass ) {
            public ResultSet<T> execute() {
                final ResultSet<T> parentRs = parent.query( entityClass )
                        .where( expression )
                        .maxResults( maxResults )
                        .firstResult( firstResult )
                        .execute();

                // unmodified
                IteratorBuilder<T> unmodifiedResults = IteratorBuilder.on( parentRs )
                        .map( entity -> entity( entityClass, entity.id() ) )
                        .filter( entity -> {
                            EntityStatus status = entity.status();
                            assert status != EntityStatus.CREATED; 
                            return status == EntityStatus.LOADED;                            
                        });
                
                // new/updated states -> pre-process
                IteratorBuilder<T> modifiedResults = (IteratorBuilder<T>)IteratorBuilder.on( modified.values() )
                        .filter( entity -> {
                            if (entity.getClass().equals( entityClass ) 
                                    && (entity.status() == CREATED || entity.status() == MODIFIED )) {
                                if (expression == null) {
                                    return true;
                                }
                                else if (expression instanceof BooleanExpression) {
                                    return expression.evaluate( entity );
                                }
                                else {
                                    return storeUow.evaluate( entity.state(), expression );
                                }
                            }
                            return false;
                        });
                
                // ResultSet, caching the ids for subsequent runs
                return new ResultSet<T>() {

                    /** null after one full run */
                    private Iterator<T>     results = unmodifiedResults.concat( modifiedResults );
                    private List<Object>    cachedIds = new ArrayList( 1024 );
                    /** The cached cachedSize; not synchronized */
                    private int             cachedSize = -1;

                    @Override
                    public Iterator<T> iterator() {
                        return new Iterator<T>() {
                            int index = -1;
                            @Override
                            public boolean hasNext() {
                                if (index+1 < cachedIds.size() || (results != null && results.hasNext())) {
                                    return true;
                                }
                                else {
                                    parentRs.close();
                                    results = null;
                                    return false;
                                }
                            }
                            @Override
                            public T next() {
                                if (++index < cachedIds.size()) {
                                    return entity( entityClass, cachedIds.get( index ), null );
                                }
                                else {
                                    assert index == cachedIds.size() : "index == cachedIds.size(): " +  index + ", " + cachedIds.size();
                                    T result = results.next();
                                    cachedIds.add( result.id() );
                                    return result;
                                }
                            }
                        };
                    }
                    
                    @Override
                    public int size() {
                        if (cachedSize == -1) {
                            cachedSize = results == null
                                    ? cachedIds.size()
                                    : modified.isEmpty() 
                                            ? parentRs.size()
                                            : Iterators.size( iterator() );
                        }
                        return cachedSize;
                    }
                    
                    @Override
                    public Stream<T> stream() {
                        // XXX use cachedIds.size() if available
                        return StreamSupport.stream( spliterator(), false );
                    }

                    @Override
                    public void close() {
                        parentRs.close();
                        results = null;
                        cachedIds = null;
                    }
                };
            }
        };
    }


    @Override
    public UnitOfWork newUnitOfWork() {
        return new UnitOfWorkNested( repo, storeUow(), this );
    }


    @Override
    public void prepare() throws IOException, ConcurrentEntityModificationException {
        checkOpen();
        commitLock.lock();
        prepareResult = null;
        lifecycle( State.BEFORE_PREPARE );
        for (Entity entity : modified.values()) {
            // created
            if (entity.status() == EntityStatus.CREATED) {
                // create a separate instance in the parent; same as for modified instances
                // this allows to manage distinct status for parent and nested entity
                CompositeState entityState = repo.contextOfEntity( entity ).getState();
                CompositeState parentState = storeUow().cloneEntityState( entityState );
                
                Entity parentEntity = parent.entityForState( entity.getClass(), parentState.getUnderlying() );
                repo.contextOfEntity( parentEntity ).raiseStatus( EntityStatus.CREATED );
                parent.modified.putIfAbsent( parentEntity.id(), parentEntity );
            }
            // modified
            if (entity.status() == EntityStatus.MODIFIED
                    || entity.status() == EntityStatus.REMOVED) {
                Entity parentEntity = parent.entity( entity.getClass(), entity.id() );
                
                if (parentEntity == null || parentEntity.status() == EntityStatus.REMOVED) {
                    throw new ConcurrentEntityModificationException( "Entity was removed in parent UnitOfWork.", singletonList( entity ) );
                }

                repo.contextOfEntity( parentEntity ).raiseStatus( entity.status() );

                CompositeState parentState = repo.contextOfEntity( parentEntity ).getState();
                CompositeState clonedState = repo.contextOfEntity( entity ).getState();
                storeUow().reincorparateEntityState( parentState, clonedState );
            }
        }
        lifecycle( State.AFTER_PREPARE );
        prepareResult = PREPARED;
    }


    @Override
    public void commit() throws ModelRuntimeException {
        // prepare if not yet done
        if (prepareResult == null) {
            try {
                prepare();
            }
            catch (Exception e) {
                Throwables.propagateIfPossible( e, ModelRuntimeException.class );
            }
        }
        if (prepareResult != PREPARED) {
            throw new ModelRuntimeException( "UnitOfWork was not successfully prepared for commit." );
        }
        prepareResult = null;
        
        // reset Entity status
        lifecycle( State.BEFORE_COMMIT );
        for (Entry<Object,Entity> entry : loaded) {
            repo.contextOfEntity( entry.getValue() ).resetStatus( EntityStatus.LOADED );
        }
        lifecycle( State.AFTER_COMMIT );
        modified.clear();
        commitLock.unlock( true );
    }


    @Override
    public void rollback() throws ModelRuntimeException {
        checkOpen();
        lifecycle( State.BEFORE_ROLLBACK );
        lifecycle( State.AFTER_ROLLBACK );
        prepareResult = null;
        loaded.clear();
        modified.clear();
        commitLock.unlock( false );
    }


    public void close() {
        if (isOpen()) {
            commitLock.unlock( false );
            parent = null;
            repo = null;
            loaded = null;
            modified = null;
        }
    }

}