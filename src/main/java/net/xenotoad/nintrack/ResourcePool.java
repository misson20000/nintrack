package net.xenotoad.nintrack;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by misson20000 on 2/25/17.
 */
public class ResourcePool<T> {
    private final BlockingQueue<T> pool;
    private final ResourceFactory<T> factory;
    private final ReentrantLock lock;

    public ResourcePool(ResourceFactory<T> factory) {
        this.factory = factory;
        this.pool = new LinkedBlockingQueue<T>();
        this.lock = new ReentrantLock();
    }

    public void use(Usage<T> function) throws InvocationTargetException {
        lock.lock();
        T resource = pool.poll();
        if(resource == null) {
            resource = factory.create();
        }
        lock.unlock();
        try {
            function.use(resource);
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            lock.lock();
            pool.add(resource); // recycle
            lock.unlock();
        }
    }

    public interface ResourceFactory<T> {
        T create();
    }

    public interface Usage<T> {
        void use(T resource) throws Exception;
    }
}
