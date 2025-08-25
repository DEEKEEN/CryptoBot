package api.utils;

import java.util.concurrent.Callable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;

public class AwaitibilityHelper {

        public static void waitForCondition(Callable<Boolean> condition, int timeout, int pollingInterval, String exception) {
            try {
                await().pollInSameThread().atMost(timeout, MILLISECONDS).ignoreExceptions().pollInterval(pollingInterval, MILLISECONDS).until(condition);
            } catch (Exception e) {
                throw new AssertionError(exception, e);
            }
        }
    }