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
package org.polymap.model2;

/**
 * Bases class of computed {@link Association} implementations. See {@link Computed}
 * annotation.
 *
 * @author Falko Bräutigam
 */
public abstract class ComputedAssociation<T extends Entity>
        extends ComputedPropertyBase<T>
        implements Association<T> {

    @Override
    public void set( T value ) {
        throw new UnsupportedOperationException( "This computed property is immutable." );
    }

    @Override
    public String toString() {
        T value = get();
        return "ComputedAssociation[name:" + info().getName() + ",value=" + (value != null ? value.toString() : "null") + "]";
    }

}
