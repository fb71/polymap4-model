/* 
 * polymap.org
 * Copyright (C) 2016, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.store.recordstore.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.model2.test.BidiAssociationTest;
import org.polymap.recordstore.IRecordStore;
import org.polymap.recordstore.lucene.LuceneRecordStore;

/**
 * The {@link BidiAssociationTest} with {@link IRecordStore}/Lucene backend.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class LuceneBidiAssociationTest
        extends BidiAssociationTest {

    private static final Log log = LogFactory.getLog( LuceneBidiAssociationTest.class );

    protected IRecordStore          store;

    
    public LuceneBidiAssociationTest( String name ) {
        super( name );
    }


    protected void setUp() throws Exception {
        super.setUp();
        try {
            store = new LuceneRecordStore();
            repo = EntityRepository.newConfiguration()
                    .store.set( new RecordStoreAdapter( store ) )
                    .entities.set( new Class[] {Group.class, Member.class} )
                    .create();
            uow = repo.newUnitOfWork();
        }
        catch (Exception e) {
            log.warn( "", e );
            throw e;
        }
    }
    
}
