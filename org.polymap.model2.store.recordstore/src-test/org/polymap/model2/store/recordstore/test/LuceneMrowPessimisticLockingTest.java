/* 
 * polymap.org
 * Copyright (C) 2016, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.store.recordstore.test;

import org.polymap.model2.Concerns;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.locking.MrowPessimisticLocking;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.model2.test.PessimisticLockingTest;
import org.polymap.recordstore.lucene.LuceneRecordStore;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class LuceneMrowPessimisticLockingTest
        extends PessimisticLockingTest {

    @Override
    protected void setUp() throws Exception {
        LuceneRecordStore store = new LuceneRecordStore();
        repo = EntityRepository.newConfiguration()
                .store.set( new RecordStoreAdapter( store ) )
                .entities.set( new Class[] {MrowLocked.class} )
                .create();
        
        setUpEntities( MrowLocked.class );
    }


    public static class MrowLocked
            extends Locked {

        @Concerns( MrowPessimisticLocking.class )
        public Property<String>     prop;

        @Override
        public String read() {
            return prop.get();
        }

        @Override
        public void write( String value ) {
            prop.set( value );
        }
    }

}
