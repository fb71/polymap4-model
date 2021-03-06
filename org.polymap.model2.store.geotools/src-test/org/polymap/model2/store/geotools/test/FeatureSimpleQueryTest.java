/* 
 * polymap.org
 * Copyright 2012, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.store.geotools.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.store.geotools.FeatureStoreAdapter;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FeatureSimpleQueryTest {
        //extends SimpleQueryTest {

    private static Log log = LogFactory.getLog( FeatureSimpleQueryTest.class );


    private FeatureStoreAdapter         store;
    
    
//    public FeatureSimpleQueryTest( String name ) {
//        super( name );
//    }
//
//
//    protected void setUp() throws Exception {
//        super.setUp();
//        
//        //File f = new File( "/home/falko/Data/WGN_SAX_INFO/Datenuebergabe_Behoerden_Stand_1001/Shapedateien/Chem_Zustand_Fliessgew_WK_Liste_CHEM_0912.shp" );
//        File dir = new File( "/tmp/" + getClass().getSimpleName() );
//        dir.mkdir();
//        File f = new File( dir, "employee.shp" );
//        log.debug( "opening shapefile: " + f );
//        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
//
//        Map<String,Serializable> params = new HashMap<String, Serializable>();
//        params.put( "url", f.toURI().toURL() );
//        params.put( "create spatial index", Boolean.TRUE );
//
//        ds = (ShapefileDataStore) dataStoreFactory.createNewDataStore( params );
//        store = new FeatureStoreAdapter( ds );
//        repo = EntityRepository.newConfiguration()
//                .store.set( store )
//                .entities.set( new Class[] {Employee.class} )
//                .create();
//        uow = repo.newUnitOfWork();
//    }

}
