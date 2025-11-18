package devkit.component.task.job.provider;

import devkit.component.task.job.model.TaskScheduleVO;

import java.util.List;

public interface ITaskDataProvider {

    /** Query all valid task schedules */
    List<TaskScheduleVO> queryAllValidTaskSchedule();
    
    /** Query all invalid task IDs */
    List<Long> queryAllInvalidTaskScheduleIds();

}