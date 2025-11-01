package devkit.component.task.job.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "devkit.component.task.job", ignoreInvalidFields = true)
public class TaskJobAutoProperties {

    /** 是否启用任务调度器 */
    private boolean enabled = true;
    
    /** 线程池大小 */
    private int poolSize = 10;
    
    /** 线程名称前缀 */
    private String threadNamePrefix = "devkit-component-task-scheduler-";
    
    /** 关闭时等待任务完成 */
    private boolean waitForTasksToCompleteOnShutdown = true;
    
    /** 等待终止时间（秒） */
    private int awaitTerminationSeconds = 60;
    
    /** 任务刷新间隔（毫秒） */
    private long refreshInterval = 60000;
    
    /** 清理无效任务的cron表达式 */
    private String cleanInvalidTasksCron = "0 0/10 * * * ?";

}