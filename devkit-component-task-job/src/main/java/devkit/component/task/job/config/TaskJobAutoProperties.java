package devkit.component.task.job.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "devkit.component.task.job", ignoreInvalidFields = true)
public class TaskJobAutoProperties {

    /** Enable scheduler */
    private boolean enabled = true;
    
    /** Pool size */
    private int poolSize = 10;
    
    /** Thread name prefix */
    private String threadNamePrefix = "devkit-component-task-scheduler-";
    
    /** Wait on shutdown */
    private boolean waitForTasksToCompleteOnShutdown = true;
    
    /** Await termination seconds */
    private int awaitTerminationSeconds = 60;
    
    /** Refresh interval (ms) */
    private long refreshInterval = 60000;
    
    /** Clean invalid tasks cron */
    private String cleanInvalidTasksCron = "0 0/10 * * * ?";

}