/* 
 * polymap.org
 * Copyright (C) 2015-2016, Falko Bräutigam. All rights reserved.
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
package org.polymap.model2.engine.cache;

import java.util.Properties;

import java.net.URI;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class SimpleCacheManager
        implements CacheManager {

    private static Log log = LogFactory.getLog( SimpleCacheManager.class );

    @Override
    public <K,V,C extends Configuration<K,V>> Cache<K,V> createCache( String cacheName, C config )
            throws IllegalArgumentException {
        return new SimpleCache( config );
    }

    @Override
    public void destroyCache( String cacheName ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public CachingProvider getCachingProvider() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public URI getURI() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public ClassLoader getClassLoader() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public Properties getProperties() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <K, V> Cache<K,V> getCache( String cacheName, Class<K> keyType, Class<V> valueType ) {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <K, V> Cache<K,V> getCache( String cacheName ) {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public Iterable<String> getCacheNames() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void enableManagement( String cacheName, boolean enabled ) {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void enableStatistics( String cacheName, boolean enabled ) {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void close() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public boolean isClosed() {
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T> T unwrap( Class<T> clazz ) {
        throw new RuntimeException( "not yet implemented." );
    }
}
