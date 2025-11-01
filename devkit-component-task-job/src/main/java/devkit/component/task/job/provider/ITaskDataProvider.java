package devkit.component.task.job.provider;

import devkit.component.task.job.model.TaskScheduleVO;

import java.util.List;

public interface ITaskDataProvider {

    /**
     * 查询所有有效的任务调度配置
     * @return 任务调度配置列表
     */
    List<TaskScheduleVO> queryAllValidTaskSchedule();
    
    /**
     * 查询所有无效的任务ID
     * @return 无效任务 ID列表
     */
    List<Long> queryAllInvalidTaskScheduleIds();

}