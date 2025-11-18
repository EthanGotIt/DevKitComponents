package devkit.component.dynamic.config.center.test.task;

import devkit.component.task.job.model.TaskScheduleVO;
import devkit.component.task.job.provider.ITaskDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TestTaskDataProvider03 implements ITaskDataProvider {

    private static final Logger log = LoggerFactory.getLogger(TestTaskDataProvider03.class);

    @Override
    public List<TaskScheduleVO> queryAllValidTaskSchedule() {
        List<TaskScheduleVO> tasks = new ArrayList<>();

        // Task 3: simple runnable
        TaskScheduleVO task3 = new TaskScheduleVO();
        task3.setId(3L);
        task3.setDescription("task-3 cleanup");
        task3.setCronExpression("0 0 2 * * ?");
        task3.setTaskParam("{\"cleanup_days\":7}");
        
        // Runnable logic
        Runnable task3Logic = () -> {
            log.info("run cleanup, id: 3");
            log.info("cleanup done");
        };

        task3.setTaskLogic(task3Logic);
        tasks.add(task3);
        
        return tasks;
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        // Invalid IDs for testing
        return Arrays.asList(999L, 1000L);
    }

}