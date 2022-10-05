package io.tempo.entitylocker.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Locker {
  public final AtomicInteger numberOfThreadsInQueue = new AtomicInteger(1);
  private final Lock lock = new ReentrantLock();

  public Locker addThreadInQueue() {
    numberOfThreadsInQueue.incrementAndGet();
    return this;
  }

  public void lock() {
    lock.lock();
  }

  public void unlock() {
    lock.unlock();
  }


  public int removeThreadFromQueue() {
    return numberOfThreadsInQueue.decrementAndGet();
  }

}
