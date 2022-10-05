package io.tempo.entitylocker;

public interface EntityLocker<T> {
  /**
   * Lock an entity by ID and execute specified code on it
   *
   * @param id            entity id
   * @param protectedCode block of code to run
   */
  void lockAndExecute(T id, ProtectedCode protectedCode);

  /**
   * Lock an entity by ID for a specific duration of time and execute specified code on it.
   * The entity is locked for timeout duration, regardless of if the protected code executes is less time.
   *
   * @param id            entity id
   * @param protectedCode block of code to run
   * @param timeout       time in ms for which id is locked
   */
  void lockAndExecute(T id, ProtectedCode protectedCode, long timeout);
}
