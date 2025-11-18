package devkit.component.task.job.service;

import devkit.component.task.job.model.TaskScheduleVO;

public interface ITaskJobService {

    /** Add a task */
    boolean addTask(TaskScheduleVO task);

    /** Remove a task */
    boolean removeTask(Long taskId);

    /** Refresh task schedules */
    void refreshTasks();
    
    /** Clean invalid tasks */
    void cleanInvalidTasks();
    
    /** Stop all tasks */
    void stopAllTasks();
    
    /** Active task count */
    int getActiveTaskCount();
    
    /** Initialize task schedules */
    void initializeTasks();

}