/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute delegate and/or modify delegate
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that delegate will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.engine;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Support for aggregated operations based on a raw {@link Iterator} using java.util
 * {@link Function} and {@link Predicate}. Used by {@link UnitOfWorkImpl} which
 * operates on store Iterators instead of (streamable) Collection. Also avoid
 * dependency on Guava.
 *
 * @deprecated
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class IteratorBuilder<T>
        implements Iterator<T> {

    public static <TT> IteratorBuilder<TT> on( Iterator<TT> delegate ) {
        return new IteratorBuilder<TT>( delegate ) {
            @Override
            public boolean hasNext() {
                return this.delegate.hasNext();
            }
            @Override
            public TT next() {
                return (TT)this.delegate.next();
            }
            @Override
            protected Iterator<TT> thisButSuppressStart() {
                return (Iterator<TT>)this.delegate;
            }
        };
    }
    
    public static <TT> IteratorBuilder<TT> on( Iterable<TT> delegate ) {
        return on( delegate.iterator() );
    }
    
    
    // instance *******************************************
    
    protected Iterator<?>           delegate;
    
    
    protected IteratorBuilder( Iterator<?> delegate ) {
        assert delegate != null;
        this.delegate = delegate;
    }

    
    protected Iterator<T> thisButSuppressStart() {
        return this;
    }

    
    /**
     * 
     * @param second
     * @return Newly created instance.
     */
    public IteratorBuilder<T> concat( Iterator<T> second ) {
        return new IteratorBuilder( thisButSuppressStart() ) {
            private boolean     isFirst = true;

            @Override
            public boolean hasNext() {
                if (isFirst) {
                    if (delegate.hasNext()) {
                        return true;
                    }
                    else {
                        isFirst = false;
                    }
                }
                return second.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                if (isFirst) {
                    try {
                        return (T)delegate.next();
                    }
                    catch (NoSuchElementException e) {
                        // ignore, check second
                    }
                }
                return second.next();
            }
        };
    }
    
    
    /**
     * 
     * @param predicate
     * @return Newly created instance.
     */
    public IteratorBuilder<T> filter( Predicate<T> predicate ) {
        return new IteratorBuilder( thisButSuppressStart() ) {
            private T           next;
            
            @Override
            public boolean hasNext() {
                next = null;
                while (delegate.hasNext()) {
                    if (predicate.test( next = (T)delegate.next() )) {
                        return true;
                    }
                }
                return false;
            }
            
            @Override
            public T next() {
                return next;
            }
        };
    }

    
    /**
     * 
     * @param f
     * @return Newly created instance.
     */
    public <TT> IteratorBuilder<TT> map( Function<T,TT> f ) {
        return new IteratorBuilder<TT>( thisButSuppressStart() ) {

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public TT next() {
                return f.apply( (T)delegate.next() );
            }
        };
    }
    
}
