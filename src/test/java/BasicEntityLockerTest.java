import io.tempo.entitylocker.BasicEntityLocker;
import io.tempo.entitylocker.EntityLocker;
import io.tempo.entitylocker.ProtectedCode;
import io.tempo.entitylocker.utils.LockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicEntityLockerTest {
  EntityLocker<Integer> integerLocker;
  EntityLocker<String> stringBasicEntityLocker;

  @BeforeEach
  void setUp() {
    integerLocker = new BasicEntityLocker<>(new LockManager<>());
    stringBasicEntityLocker = new BasicEntityLocker<>(new LockManager<>());
  }

  @Test
  @DisplayName("EntityLocker should support different types of entity IDs")
  void entityLockerCanWorkWithDifferentIdTypes() throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(2);

    stringBasicEntityLocker.lockAndExecute("test_id", code(1000, latch));
    integerLocker.lockAndExecute(1, code(1000, latch));

    // The 5ms added is to account for extra time taken by the system to run
    boolean completed = latch.await(1010, TimeUnit.MILLISECONDS);
    assertTrue(completed);
  }

  @Test
  @DisplayName("Should allow concurrent execution of protected code on different entities.")
  void entitiesWithDifferentIdsRunSimultaneously() throws Exception {

    CountDownLatch latch = new CountDownLatch(6);
    ProtectedCode code = code(1000, latch);

    integerLocker.lockAndExecute(1, code);
    integerLocker.lockAndExecute(2, code);
    integerLocker.lockAndExecute(3, code);
    integerLocker.lockAndExecute(4, code);
    integerLocker.lockAndExecute(5, code);
    integerLocker.lockAndExecute(6, code);

    // check if latch has been called 6 times in 1s.
    // The 5ms added is to account for extra time taken by the system to run
    boolean completed = latch.await(1005, TimeUnit.MILLISECONDS);

    assertTrue(completed);
  }

  @Test
  @DisplayName("For any given entity, " +
      "EntityLocker should guarantee that at most one thread executes protected code on that entity. " +
      "If there’s a concurrent request to lock the same entity, " +
      "the other thread should wait until the entity becomes available.")
  void entitiesWithSimilarIdsWaitForOthersToCompleteBeforeRunning() throws Exception {

    CountDownLatch latch = new CountDownLatch(3);

    integerLocker.lockAndExecute(1, code(1000, latch));
    integerLocker.lockAndExecute(1, code(1000, latch));
    integerLocker.lockAndExecute(1, code(1000, latch));

    // Multiple latch.await calls with timeout add up sequentially
    // 10ms added to each timeout as buffer
    boolean after1s = latch.await(1010, TimeUnit.MILLISECONDS);
    boolean after2s = latch.await(1010, TimeUnit.MILLISECONDS);
    boolean after3s = latch.await(1010, TimeUnit.MILLISECONDS);

    assertFalse(after1s);
    assertFalse(after2s);
    assertTrue(after3s);
  }

  @ParameterizedTest
  @CsvSource({
      "300, 100, 500",
      "100, 1000, 500"
  })
  @DisplayName("Allow the caller to specify timeout for locking an entity.")
  void lockEntityForSpecifiedTime(
      int timeout,
      int delayForFirstExecution,
      int delayForSecondExecution
  ) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(
        timeout >= delayForFirstExecution ? 2 : 1
    );

    long startTime = System.currentTimeMillis();
    integerLocker.lockAndExecute(1, code(delayForFirstExecution, latch), timeout);

    // Adding 50ms sleep to guarantee order of execution.
    // Sleep time does not affect total execution time if timeout for first invocation > sleep time
    Thread.sleep(50);

    integerLocker.lockAndExecute(1, code(delayForSecondExecution, latch));

    latch.await();

    double elapsedTime = (double) System.currentTimeMillis() - startTime;
    double expectedTime = (double) timeout + delayForSecondExecution;

    assertThat(elapsedTime, closeTo(expectedTime, 50));
  }

  private ProtectedCode code(int delay, CountDownLatch countDownLatch) {
    return () -> {
      Thread.sleep(delay);
      countDownLatch.countDown();
    };
  }
}
