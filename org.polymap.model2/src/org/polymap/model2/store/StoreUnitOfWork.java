/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.store;

import org.polymap.model2.Entity;
import org.polymap.model2.engine.UnitOfWorkImpl;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * Represents the store interface provided by an underlying store to be
 * used by a front-end {@link UnitOfWork}. 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface StoreUnitOfWork {

    /**
     *
     * @param id The identifier of the Entity.
     * @param entityClass
     * @return The {@link CompositeState}, or null if no entity exists for the given identifier. 
     */
    public <T extends Entity> CompositeState loadEntityState( Object id, Class<T> entityClass );

    public <T extends Entity> CompositeState adoptEntityState( Object state, Class<T> entityClass );

    /**
     * 
     * 
     * @param id The identifier of the newly created entity, or null if a new
     *        identifier is to be created by the store automatically.
     * @param entityClass
     * @return Newly created {@link CompositeState}.
     */
    public <T extends Entity> CompositeState newEntityState( Object id, Class<T> entityClass );

    /**
     * 
     */
    public StoreResultSet executeQuery( Query query );
    
    /**
     * Evaluate the given store specific expression against the given Composite
     * state. This method is used by {@link UnitOfWorkImpl} to blend a query result
     * with the locally modified features.
     * 
     * @param entityState
     * @param expression
     * @return True if expression is true for given entity.
     */
    public boolean evaluate( Object entityState, Object expression );

    public void prepareCommit( Iterable<Entity> loaded ) throws Exception;
    
    public void commit();
    
    public void close();

    public void rollback();

}
