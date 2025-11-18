package devkit.component.dynamic.config.center.test.task;

import devkit.component.task.job.model.TaskScheduleVO;
import devkit.component.task.job.service.ITaskJobService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskJobTest {

    private final Logger log = LoggerFactory.getLogger(TaskJobTest.class);

    @Resource
    private ITaskJobService taskJobService;

    /** Refresh tasks */
    @Test
    public void testRefreshTasks() {
        log.info("Refresh test start");
        
        try {
            taskJobService.refreshTasks();
            
            int activeTaskCount = taskJobService.getActiveTaskCount();
            log.info("Active: {}", activeTaskCount);
            
            // Wait
            Thread.sleep(60000);
            
        } catch (Exception e) {
            log.error("Refresh test failed", e);
        }
        
        log.info("Refresh test end");
    }

    /** Clean invalid tasks */
    @Test
    public void testCleanInvalidTasks() {
        log.info("Clean test start");
        
        try {
            taskJobService.refreshTasks();
            
            int beforeCount = taskJobService.getActiveTaskCount();
            log.info("Active before: {}", beforeCount);
            
            taskJobService.cleanInvalidTasks();
            
            int afterCount = taskJobService.getActiveTaskCount();
            log.info("Active after: {}", afterCount);
            
        } catch (Exception e) {
            log.error("Clean test failed", e);
        }
        
        log.info("Clean test end");
    }

    /** Stop all */
    @Test
    public void testStopAllTasks() {
        log.info("Stop test start");
        
        try {
            taskJobService.refreshTasks();
            
            int beforeCount = taskJobService.getActiveTaskCount();
            log.info("Active before: {}", beforeCount);
            
            taskJobService.stopAllTasks();
            
            int afterCount = taskJobService.getActiveTaskCount();
            log.info("Active after: {}", afterCount);
            
        } catch (Exception e) {
            log.error("Stop test failed", e);
        }
        
        log.info("Stop test end");
    }

    /** Remove task */
    @Test
    public void testRemoveTask() {
        log.info("Remove test start");
        
        try {
            TaskScheduleVO testTask = new TaskScheduleVO();
            testTask.setId(4L);
            testTask.setDescription("task to remove");
            testTask.setCronExpression("*/20 * * * * *");
            testTask.setTaskParam("{\"message\":\"will be removed\"}");
            
            testTask.setTaskLogic(() -> {
                log.info("Run test task id: {}, param: {}", testTask.getId(), testTask.getTaskParam());
            });

            boolean addResult = taskJobService.addTask(testTask);
            log.info("Add: {}", addResult);
            
            int beforeCount = taskJobService.getActiveTaskCount();
            log.info("Active before: {}", beforeCount);
            
            Thread.sleep(5000);
            
            boolean removeResult = taskJobService.removeTask(4L);
            log.info("Remove: {}", removeResult);
            
            int afterCount = taskJobService.getActiveTaskCount();
            log.info("Active after: {}", afterCount);
            
        } catch (Exception e) {
            log.error("Remove test failed", e);
        }
        
        log.info("Remove test end");
    }

    /** Integration test */
    @Test
    public void testTaskJobIntegration() {
        log.info("Integration test start");

        try {
            // 1. Refresh
            taskJobService.refreshTasks();
            log.info("Active: {}", taskJobService.getActiveTaskCount());

            // 2. Wait
            Thread.sleep(3000);

            // 3. Clean
            taskJobService.cleanInvalidTasks();
            log.info("Active: {}", taskJobService.getActiveTaskCount());

            // 4. Wait again
            Thread.sleep(3000);

            // 5. Stop
            taskJobService.stopAllTasks();
            log.info("Active: {}", taskJobService.getActiveTaskCount());

        } catch (Exception e) {
            log.error("Integration test failed", e);
        }

        log.info("Integration test end");
    }

}