package devkit.component.task.job.model;

import lombok.Data;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Data
public class TaskScheduleVO {

    /** Task ID */
    private Long id;
    
    /** Description */
    private String description;
    
    /** Cron */
    private String cronExpression;
    
    /** Params */
    private String taskParam;
    
    /** Executor supplier */
    private Supplier<Runnable> taskExecutor;

    public TaskScheduleVO() {
    }

    /** Convenience: set task logic */
    public void setTaskLogic(Runnable taskLogic) {
        this.taskExecutor = () -> taskLogic;
    }
    
    /** Convenience: set logic with params */
    public void setTaskLogic(BiConsumer<Long, String> taskLogic) {
        this.taskExecutor = () -> () -> taskLogic.accept(this.id, this.taskParam);
    }

    @Override
    public String toString() {
        return "TaskScheduleVO{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                ", taskParam='" + taskParam + '\'' +
                ", hasTaskExecutor=" + (taskExecutor != null) +
                '}';
    }
}