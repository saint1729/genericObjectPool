package com.turvo.objectpool;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ObjectPoolTest
{
	public class TestObject {

	    private String name;
	    private long id = 0;

	    public TestObject(String name, long id) {
	        this.name = name;
	        this.id = id;

	        for (int i = 0; i < Integer.MAX_VALUE; i++) {}

	        System.out.println("Object with process no. " + id + " was created");
	    }

	    public String getName() {
	        return name;
	    }

	    public long getId() {
	        return id;
	    }
	}
	
	public class TestTask implements Runnable {

	    private ObjectPool<TestObject> pool;

	    private int threadNo;

	    public TestTask(ObjectPool<TestObject> pool, int threadNo) {
	        this.pool = pool;
	        this.threadNo = threadNo;
	    }

	    public void run() {
	    	TestObject testObject = pool.tryBorrowObject(5000);

	        System.out.println("Thread " + threadNo + 
	                ": Object with process no. " + testObject.getId() + " was borrowed");

	        for (int i = 0; i < 100000; i++) {}

	        pool.releaseObject(testObject);

	        System.out.println("Thread " + threadNo + 
	                ": Object with process id. " + testObject.getId() + " was returned");
	    }
	}
	
    private ObjectPoolService<TestObject> pool;

    private AtomicLong id = new AtomicLong(0);

    @After
    public void shutDown() {
        pool.shutdown();
    }

    @Test
    public void testObjectPool1() {
    	pool = new ObjectPoolService<TestObject>(4,10)
        {
            protected TestObject createObject() {
                return new TestObject("test1", id.incrementAndGet());
            }
        };
        
        assertEquals(4, pool.minIdle());
        assertEquals(10, pool.maxIdle());

        assertEquals(4, pool.created());
        assertEquals(4, pool.availableActive());
        assertEquals(10, pool.availableTotal());
        assertEquals(0, pool.borrowed());
        
        ExecutorService executor = Executors.newFixedThreadPool(8);

        executor.execute(new TestTask(pool, 1));
        executor.execute(new TestTask(pool, 2));
        executor.execute(new TestTask(pool, 3));
        executor.execute(new TestTask(pool, 4));
        executor.execute(new TestTask(pool, 5));
        executor.execute(new TestTask(pool, 6));
        executor.execute(new TestTask(pool, 7));
        executor.execute(new TestTask(pool, 8));

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testObjectPool2() {
    	pool = new ObjectPoolService<TestObject>(1,10)
        {
            protected TestObject createObject() {
                return new TestObject("test2", id.incrementAndGet());
            }
        };
        
        assertEquals(1, pool.minIdle());
        assertEquals(10, pool.maxIdle());

        assertEquals(1, pool.created());
        assertEquals(1, pool.availableActive());
        assertEquals(10, pool.availableTotal());
        assertEquals(0, pool.borrowed());
        
        TestObject obj1 = pool.borrowObject();
        assertNotNull(obj1);
        assertEquals(1, pool.created());
        assertEquals(0, pool.availableActive());
        assertEquals(9, pool.availablePassive());
        assertEquals(1, pool.borrowed());
        
        pool.shutdown();
       
    }
    
    @Test
    public void testObjectPool3() {
    	pool = new ObjectPoolService<TestObject>(1,10)
        {
            protected TestObject createObject() {
                return new TestObject("test3", id.incrementAndGet());
            }
        };
        
        TestObject obj1;
        TestObject[] objs = new TestObject[10];
        for (int i = 0; i < 10; i++) {
            objs[i] = pool.borrowObject();
            assertNotNull(objs[i]);
        }
        
        obj1 = pool.tryBorrowObject(5000);
        assertNull(obj1);
        assertEquals(10, pool.created());
        assertEquals(0, pool.availablePassive());
        assertEquals(0, pool.availableTotal());
        assertEquals(10, pool.borrowed());

        for (int i = 0; i < 6; i++) {
            pool.releaseObject(objs[i]);
        }
        
        assertEquals(10, pool.created());
        assertEquals(0, pool.availablePassive());
        assertEquals(6, pool.availableTotal());
        assertEquals(4, pool.borrowed());

        for (int i = 6; i < 10; i++) {
            pool.releaseObject(objs[i]);
        }
        
        assertEquals(10, pool.created());
        assertEquals(0, pool.availablePassive());
        assertEquals(10, pool.availableTotal());
        assertEquals(0, pool.borrowed());
       
        pool.shutdown();
    }
    
    @Test
    public void testObjectPool4() {
    	pool = new ObjectPoolService<TestObject>(2,4)
        {
            protected TestObject createObject() {
                return new TestObject("test4", id.incrementAndGet());
            }
        };
        
        TestObject[] objs = new TestObject[4];
        for (int i = 0; i < 4; i++) {
            objs[i] = pool.borrowObject();
            assertNotNull(objs[i]);
        }
        
        assertEquals(4, pool.created());
        assertEquals(0, pool.availablePassive());
        assertEquals(0, pool.availableTotal());
        assertEquals(4, pool.borrowed());
        
        pool.shutdown();
        
        assertEquals(0, pool.created());
        assertEquals(0, pool.availablePassive());
        assertEquals(0, pool.availableTotal());
        assertEquals(0, pool.borrowed());
       
    }
}
