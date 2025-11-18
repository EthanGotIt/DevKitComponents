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
public class TestTaskDataProvider02 implements ITaskDataProvider {

    private static final Logger log = LoggerFactory.getLogger(TestTaskDataProvider02.class);

    @Override
    public List<TaskScheduleVO> queryAllValidTaskSchedule() {
        List<TaskScheduleVO> tasks = new ArrayList<>();

        // Task 2: functional
        TaskScheduleVO task2 = new TaskScheduleVO();
        task2.setId(2L);
        task2.setDescription("task-2 report");
        task2.setCronExpression("0/10 * * * * ?");
        task2.setTaskParam("{\"report_type\":\"daily\",\"format\":\"pdf\"}");
        
        // BiConsumer logic
        BiConsumer<Long, String> task2Logic = (taskId, taskParam) -> {
            log.info("run task-2, id: {}, param: {}", taskId, taskParam);
            try {
                Thread.sleep(1000);
                log.info("task-2 done");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("task-2 interrupted", e);
            }
        };
        task2.setTaskLogic(task2Logic);
        tasks.add(task2);

        return tasks;
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        // Invalid IDs for testing
        return Arrays.asList(999L, 1000L);
    }

}