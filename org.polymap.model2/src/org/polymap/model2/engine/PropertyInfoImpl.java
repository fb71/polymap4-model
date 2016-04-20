/* 
 * polymap.org
 * Copyright (C) 2012-2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.engine;

import java.util.Optional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Computed;
import org.polymap.model2.Description;
import org.polymap.model2.Immutable;
import org.polymap.model2.ManyAssociation;
import org.polymap.model2.MaxOccurs;
import org.polymap.model2.NameInStore;
import org.polymap.model2.Nullable;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.Queryable;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.PropertyInfo;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PropertyInfoImpl<T>
        implements PropertyInfo<T> {

    private Field                   field;

    
    public PropertyInfoImpl( Field field ) {
        assert PropertyBase.class.isAssignableFrom( field.getType() );
        this.field = field;
    }

    Field getField() {
        return field;
    }

    @Override
    public Class<T> getType() {
        Optional<Class<T>> opt = ValueInitializer.rawTypeParameter( field.getGenericType() );
        return opt.orElseThrow( () -> new ModelRuntimeException( "Type param missing: " + toString() ) );
    }
    
    @Override
    public Optional<ParameterizedType> getParameterizedType() {
        ParameterizedType declaredType = (ParameterizedType)field.getGenericType();
        Type result = declaredType.getActualTypeArguments()[0];
        return result instanceof ParameterizedType 
                ? Optional.of( (ParameterizedType)result ) : Optional.empty();
    }
    
    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public Optional<String> getDescription() {
        Description a = getAnnotation( Description.class );
        return a != null ? Optional.of( a.value() ) : Optional.empty();
    }

    @Override
    public String getNameInStore() {
        return field.getAnnotation( NameInStore.class ) != null
                ? field.getAnnotation( NameInStore.class ).value()
                : field.getName();
    }

    @Override
    public boolean isAssociation() {
        return Association.class.isAssignableFrom( field.getType() )
                || ManyAssociation.class.isAssignableFrom( field.getType() );
    }

    @Override
    public boolean isNullable() {
        return field.getAnnotation( Nullable.class ) != null;
    }

    @Override
    public boolean isImmutable() {
        return field.getAnnotation( Immutable.class ) != null;
    }

    @Override
    public boolean isComputed() {
        return field.getAnnotation( Computed.class ) != null;
    }

    @Override
    public boolean isQueryable() {
        return field.getAnnotation( Queryable.class ) != null;
    }

    @Override
    public int getMaxOccurs() {
        if (CollectionProperty.class.isAssignableFrom( field.getType() )
                || ManyAssociation.class.isAssignableFrom( field.getType() )) {
            return field.getAnnotation( MaxOccurs.class ) != null
                    ? field.getAnnotation( MaxOccurs.class ).value()
                    : Integer.MAX_VALUE;
        }
        else {
            assert field.getAnnotation( MaxOccurs.class ) == null : "@MaxOccurs is not allowed on single value properties.";
            return 1;
        }
    }

    @Override
    public T getDefaultValue() {
        return (T)DefaultValues.valueOf( field );
    }

    @Override    
    public <P extends PropertyBase<T>> P get( Composite composite ) {
        if (!field.isAccessible()) { 
            field.setAccessible( true ); 
        }
        try {
            return (P)field.get( composite );
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public <A extends Annotation> A getAnnotation( Class<A> type ) {
        return field.getAnnotation( type );
    }

    @Override
    public String toString() {
        return "PropertyInfoImpl[" + field + "]";
    }
    
}
