package io.tempo.entitylocker;

import io.tempo.entitylocker.utils.LockManager;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class BasicEntityLocker<T> implements EntityLocker<T> {

  private final LockManager<T> lockManager;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

  public BasicEntityLocker(LockManager<T> lockManager) {
    this.lockManager = lockManager;
  }

  @Override
  public void lockAndExecute(T id, ProtectedCode protectedCode) {
    new Thread(() -> runCode(id, protectedCode)).start();
  }

  @Override
  public void lockAndExecute(T id, ProtectedCode protectedCode, long timeout) {
    new Thread(() -> runCode(id, protectedCode, timeout)).start();
  }

  private void runCode(T id, ProtectedCode protectedCode) {
    try {
      lockManager.lock(id);
      protectedCode.execute();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lockManager.unlock(id);
    }
  }

  private void runCode(T id, ProtectedCode protectedCode, long timeout) {
    Future<?> future = executor.submit(() -> {
      try {
        protectedCode.execute();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    try {
      lockManager.lock(id);
      Thread.sleep(timeout);
      if (!future.isDone()) {
        future.cancel(true);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lockManager.unlock(id);
    }
  }

}
