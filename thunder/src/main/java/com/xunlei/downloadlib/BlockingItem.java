package com.xunlei.downloadlib;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingItem<T> {
    final Lock lock = new ReentrantLock();
    final Condition notEmpty = lock.newCondition();

    private volatile T item;

    public void put(T x) {
        lock.lock();
        try {
            item = x;
            if (x != null)
                notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (item == null)
                notEmpty.await();
            T t = item;
            item = null;
            return t;
        } finally {
            lock.unlock();
        }
    }

    public T tryTake(long waitMs) throws InterruptedException {
        lock.lock();
        try {
            while (item == null)
                if (!notEmpty.await(waitMs, TimeUnit.MILLISECONDS))
                    return null;
            T t = item;
            item = null;
            return t;
        } finally {
            lock.unlock();
        }
    }

    public T peek() {
        return item;
    }
}