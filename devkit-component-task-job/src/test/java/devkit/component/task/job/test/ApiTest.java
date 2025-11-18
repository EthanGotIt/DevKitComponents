package devkit.component.task.job.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.concurrent.CountDownLatch;

public class ApiTest {

    private static final Logger log = LoggerFactory.getLogger(ApiTest.class);

    @Test
    public void test() throws InterruptedException {
        // Init scheduler
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("test-task-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        // Add task
        scheduler.schedule(() -> log.info("123"), new CronTrigger("0/3 * * * * ?"));

        // Add task
        scheduler.schedule(() -> log.info("321"), new CronTrigger("0/3 * * * * ?"));

        new CountDownLatch(1).await();
    }

}
