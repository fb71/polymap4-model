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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.engine.InstanceBuilder;
import org.polymap.model2.engine.UnitOfWorkImpl;
import org.polymap.model2.engine.cache.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Simple cache based on {@link ConcurrentHashMap} or
 * {@link ConcurrentReferenceHashMap} depending on cache configuration. This is
 * the default used by {@link UnitOfWorkImpl} and for {@link InstanceBuilder}.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class SimpleCache<K,V>
        implements Cache<K,V> {

    private static Log log = LogFactory.getLog( SimpleCache.class );

    public static final int         INITIAL_SIZE = 256;
    public static final int         CONCURRENCY = 4;
    
    private ConcurrentMap<K,V>      entries;
    
    private CacheLoader<K,V>        loader;
    
    
    public SimpleCache( Configuration config ) {
        if (config instanceof CompleteConfiguration) {
            CompleteConfiguration cconfig = (CompleteConfiguration)config;
            
            // expiry
            Factory<ExpiryPolicy> expiryFactory = cconfig.getExpiryPolicyFactory();
            ExpiryPolicy expiryPolicy = expiryFactory.create();
            if (expiryPolicy.getExpiryForCreation().equals( Duration.ETERNAL )) {
                entries = new ConcurrentHashMap( INITIAL_SIZE );
            }
            else {
                entries = new ConcurrentReferenceHashMap( INITIAL_SIZE, 0.75f, CONCURRENCY, ReferenceType.STRONG, ReferenceType.SOFT, null );
            }
            
            // loader
            Factory<CacheLoader<K,V>> loaderFactory = cconfig.getCacheLoaderFactory();
            if (loaderFactory != null) {
                loader = loaderFactory.create();
            }
        }
    }

    protected void checkOpen() {
        if (entries == null) {
            throw new IllegalStateException( "Cache is closed." );
        }
    }
    
    @Override
    public V get( K key ) {
        checkOpen();
        V value = entries.get( key );
        if (value != null) {
            return value;
        }
        else if (loader != null) {
            // we do not prevent threads from concurrently creating a value for the
            // same key! but we make sure that just one value is returned to all threads
            value = loader.load( key );
            if (value != null) {
                V previous = entries.putIfAbsent( key, value );
                value = previous != null ? previous : value;
            }
            return value;
        }
        return null;
    }

    @Override
    public Map<K,V> getAll( Set<? extends K> keys ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public boolean containsKey( K key ) {
        return entries.containsKey( key );
    }

    @Override
    public void loadAll( Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void put( K key, V value ) {
        entries.put( key, value );
    }

    @Override
    public V getAndPut( K key, V value ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void putAll( Map<? extends K,? extends V> map ) {
        entries.putAll( map );
    }

    @Override
    public boolean putIfAbsent( K key, V value ) {
        return entries.putIfAbsent( key, value ) == null;
    }

    @Override
    public boolean remove( K key ) {
        return entries.remove( key ) != null;
    }

    @Override
    public boolean remove( K key, V oldValue ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public V getAndRemove( K key ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public boolean replace( K key, V oldValue, V newValue ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public boolean replace( K key, V value ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public V getAndReplace( K key, V value ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void removeAll( Set<? extends K> keys ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void removeAll() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public <C extends Configuration<K,V>> C getConfiguration( Class<C> clazz ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T> T invoke( K key, EntryProcessor<K,V,T> entryProcessor, Object... arguments )
            throws EntryProcessorException {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public <T> Map<K,EntryProcessorResult<T>> invokeAll( Set<? extends K> keys, EntryProcessor<K,V,T> entryProcessor,
            Object... arguments ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public String getName() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public CacheManager getCacheManager() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void close() {
        entries = null;
    }

    @Override
    public boolean isClosed() {
        return entries != null;
    }

    @Override
    public <T> T unwrap( Class<T> clazz ) {
        return clazz.cast( entries );
    }

    @Override
    public void registerCacheEntryListener( CacheEntryListenerConfiguration<K,V> cacheEntryListenerConfiguration ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public void deregisterCacheEntryListener( CacheEntryListenerConfiguration<K,V> cacheEntryListenerConfiguration ) {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public Iterator<javax.cache.Cache.Entry<K,V>> iterator() {
        return entries.entrySet().stream()
            .<Cache.Entry<K,V>>map( entry -> new Cache.Entry<K,V>() {
                @Override
                public K getKey() { return entry.getKey(); }
                @Override
                public V getValue() { return entry.getValue(); }
                @Override
                public <T> T unwrap( Class<T> clazz ) { throw new IllegalArgumentException( "Guava does not provide an Entry type." ); } 
            })
            .iterator();
    }
    
}
