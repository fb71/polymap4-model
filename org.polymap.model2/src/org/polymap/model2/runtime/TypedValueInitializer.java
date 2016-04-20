/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.runtime;

import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Property;

/**
 * Allows to initialize a newly created Composite value of a {@link Property} or
 * {@link CollectionProperty} with a sub-class of the declared type of the Property.
 * <p/>
 * Unfortunatelly this cannot be an (functional) interface as we use the generic
 * abstract class to get the actual type parameter.
 *
 * @deprecated Use a non-lambda {@link ValueInitializer}. 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class TypedValueInitializer<T>
        implements ValueInitializer<T> {

}
