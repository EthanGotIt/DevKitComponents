package devkit.component.dynamic.config.center.test.task;

import devkit.component.task.job.model.TaskScheduleVO;
import devkit.component.task.job.provider.ITaskDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@Service
public class TestTaskDataProvider01 implements ITaskDataProvider {

    private static final Logger log = LoggerFactory.getLogger(TestTaskDataProvider01.class);

    @Override
    public List<TaskScheduleVO> queryAllValidTaskSchedule() {
        List<TaskScheduleVO> tasks = new ArrayList<>();
        
        // Task 1: functional
        TaskScheduleVO task1 = new TaskScheduleVO();
        task1.setId(1L);
        task1.setDescription("task-1 data process");
        task1.setCronExpression("0/5 * * * * ?");
        task1.setTaskParam("{\"type\":\"data_process\",\"batch_size\":100}");
        
        // BiConsumer logic
        BiConsumer<Long, String> task1Logic = (taskId, taskParam) -> {
            log.info("run task-1, id: {}, param: {}", taskId, taskParam);
            try {
                Thread.sleep(500);
                log.info("task-1 done");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("task-1 interrupted", e);
            }
        };
        task1.setTaskLogic(task1Logic);

        tasks.add(task1);
        
        return tasks;
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        // Invalid IDs for testing
        return Arrays.asList(999L, 1000L);
    }

}