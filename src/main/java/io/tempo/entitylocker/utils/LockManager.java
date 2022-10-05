package io.tempo.entitylocker.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager<T> {

  public final Map<T, Locker> lockedIds = new ConcurrentHashMap<>();

  public void lock(T id) {
    Locker locker = lockedIds.compute(id,
        (k, existingLocker) -> existingLocker == null ? new Locker() : existingLocker.addThreadInQueue());
    locker.lock();
  }

  public void unlock(T id) {
    Locker locker = lockedIds.get(id);
    locker.unlock();
    if (locker.removeThreadFromQueue() == 0) {
      lockedIds.remove(id, locker);
    }
  }

}
