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

        // 使用简单Runnable的示例
        TaskScheduleVO task3 = new TaskScheduleVO();
        task3.setId(3L);
        task3.setDescription("测试任务3 - 清理任务");
        task3.setCronExpression("0 0 2 * * ?"); // 每天凌晨2点执行
        task3.setTaskParam("{\"cleanup_days\":7}");
        
        // 使用Runnable方式设置任务逻辑
        Runnable task3Logic = () -> {
            log.info("执行清理任务 - 任务ID: 3");
            // 模拟清理操作
            log.info("清理任务执行完成");
        };

        task3.setTaskLogic(task3Logic);
        tasks.add(task3);
        
        return tasks;
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        // 返回一些无效的任务ID用于测试
        return Arrays.asList(999L, 1000L);
    }

}