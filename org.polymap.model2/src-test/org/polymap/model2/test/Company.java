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
package org.polymap.model2.test;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Concerns;
import org.polymap.model2.Entity;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.MaxOccurs;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;

/**
 * A complex entity.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Company
        extends Entity {

    public static Company                   TYPE;
    
    protected Property<String>              name;
    
    @Nullable
    protected Association<Employee>         chief;

    @Nullable
    protected Property<Address>             address;
    
    protected CollectionProperty<Address>   moreAddresses;
    
    @MaxOccurs(100)
    @Concerns( {LogConcern.class} )
    protected CollectionProperty<String>    docs;

    protected ManyAssociation<Employee>     employees;
    
    protected CollectionProperty<Person>    fellows;
    
    protected Property<Person>              bigFellow;
    

    public void addEmployee( final Employee employee ) {
        employees.add( employee );
        
//        employees.createElement( new ValueInitializer<Association<Employee>>() {
//            @Override
//            public Association<Employee> initialize( Association<Employee> proto ) throws Exception {
//                proto.set( employee );
//                return proto;
//            }
//        });
    }
}
