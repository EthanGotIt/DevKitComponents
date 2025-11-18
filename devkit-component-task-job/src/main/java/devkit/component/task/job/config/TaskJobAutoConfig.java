package devkit.component.task.job.config;

import devkit.component.task.job.TaskJob;
import devkit.component.task.job.provider.ITaskDataProvider;
import devkit.component.task.job.service.ITaskJobService;
import devkit.component.task.job.service.TaskJobService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(TaskJobAutoProperties.class)
@ConditionalOnProperty(prefix = "devkit.component.task.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TaskJobAutoConfig {

    /** Task scheduler */
    @Bean("devkitComponentTaskScheduler")
    public TaskScheduler taskScheduler(TaskJobAutoProperties properties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(properties.getPoolSize());
        scheduler.setThreadNamePrefix(properties.getThreadNamePrefix());
        scheduler.setWaitForTasksToCompleteOnShutdown(properties.isWaitForTasksToCompleteOnShutdown());
        scheduler.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        scheduler.initialize();
        
        return scheduler;
    }

    @Bean
    public ITaskJobService taskJobService(TaskScheduler devkitComponentTaskScheduler, List<ITaskDataProvider> taskDataProviders) {
        // Init schedules
        TaskJobService taskJobService = new TaskJobService(devkitComponentTaskScheduler, taskDataProviders);
        taskJobService.initializeTasks();

        return taskJobService;
    }

    /** Scheduled maintenance */
    @Bean
    public TaskJob taskJob(TaskJobAutoProperties properties, ITaskJobService taskJobService) {
        return new TaskJob(properties, taskJobService);
    }

}