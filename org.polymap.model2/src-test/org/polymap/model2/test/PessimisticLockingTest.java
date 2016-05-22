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
package org.polymap.model2.test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Entity;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.locking.DeadlockException;
import org.polymap.model2.runtime.locking.PessimisticLocking;

import junit.framework.TestCase;

/**
 * 
 *
 * @author Falko Bräutigam
 */
public class PessimisticLockingTest
        extends TestCase {

    private static final Log log = LogFactory.getLog( PessimisticLockingTest.class );

    protected EntityRepository      repo;
    
    protected Locked                _e1, _e2;

    
    protected <T extends Locked> void setUpEntities( Class<T> cl ) throws Exception {
        UnitOfWork uow = repo.newUnitOfWork();
        _e1 = uow.createEntity( cl, null, (T proto) -> {
            proto.write( "1:initial" );
            return proto;
        });
        _e2 = uow.createEntity( cl, null, (T proto) -> {
            proto.write( "2:initial" );
            return proto;
        });
        uow.commit();
        uow.close();
    }

    
    public void testOneUow() throws Exception {
        UnitOfWork uow = repo.newUnitOfWork();
        Locked e1 = uow.entity( _e1 );

        log.info( e1.read() );
        log.info( e1.read() );
        e1.write( "modified" );
        log.info( e1.read() );
    }
    
    
    public void testConcurrentRead() throws Exception {
        UnitOfWork uow1 = repo.newUnitOfWork();
        Locked e1 = uow1.entity( _e1 );
        log.info( e1.read() );
        
        Thread t = new Thread( () -> {
            UnitOfWork uow2 = repo.newUnitOfWork();
            Locked e2 = uow2.entity( _e1 );
            log.info( e2.read() );            
        });
        t.start();
    }
    
    
    public void testConcurrentReadWriteRead() throws Exception {
        AtomicReference<String> before = new AtomicReference();
        AtomicReference<String> after = new AtomicReference();
                
        // read - sleep - read
        Thread t1 = new Thread( () -> {
            UnitOfWork uow = repo.newUnitOfWork();
            Locked e1 = uow.entity( _e1 );
            before.set( e1.read() );
            try { Thread.sleep( 1000 ); } catch (InterruptedException e) { }
            uow.close();
            PessimisticLocking.notifyClosed( uow );
            uow = null;
            
            uow = repo.newUnitOfWork();
            e1 = uow.entity( _e1 );
            after.set( e1.read() );
            uow.close();
            PessimisticLocking.notifyClosed( uow );
        }, "reader" );
        t1.start();
        
        // write
        Thread.sleep( 100 );
        Thread t2 = new Thread( () -> {
            UnitOfWork uow = repo.newUnitOfWork();
            Locked e1 = uow.entity( _e1 );
            log.info( e1.read() );
            e1.write( "modified" );
            log.info( e1.read() );
            uow.commit();
            uow.close();
            PessimisticLocking.notifyClosed( uow );
        }, "writer" );
        t2.start();
        
        t1.join();
        t2.join();
        assertEquals( before.get(), after.get() );
    }
    

    public void _testDeadlock() throws Exception {
        AtomicBoolean deadlocked = new AtomicBoolean();
        // t1
        Thread t1 = new Thread( () -> {
            UnitOfWork uow = repo.newUnitOfWork();
            uow.entity( _e1 ).read();
            try { 
                Thread.sleep( 100 ); 
                uow.entity( _e2 ).read();
            } 
            catch (InterruptedException e) { }
            catch (DeadlockException e) { deadlocked.set( true ); }
        }, "t1" );
        t1.start();
        
        // t2
        Thread t2 = new Thread( () -> {
            UnitOfWork uow = repo.newUnitOfWork();
            uow.entity( _e2 ).read();
            try { 
                Thread.sleep( 100 ); 
                uow.entity( _e1 ).read();
            } 
            catch (InterruptedException e) { }
            catch (DeadlockException e) { deadlocked.set( true ); }
        }, "t2" );
        t2.start();
        
        t1.join();
        t2.join();
        assertEquals( true, deadlocked.get() );
    }

    public static abstract class Locked
            extends Entity {
        
        public abstract String read();
        
        public abstract void write( String value );
        
    }
    
}
