package devkit.component.task.job.service;

import devkit.component.task.job.model.TaskScheduleVO;
import devkit.component.task.job.provider.ITaskDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class TaskJobService implements ITaskJobService, DisposableBean {

    private final Logger log = LoggerFactory.getLogger(TaskJobService.class);

    private final TaskScheduler taskScheduler;
    private final List<ITaskDataProvider> taskDataProviders;

    /** Map of taskId -> scheduled future */
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Set<Long> manualTaskIds = ConcurrentHashMap.newKeySet();

    /** Constructor */
    public TaskJobService(TaskScheduler taskScheduler,
                         List<ITaskDataProvider> taskDataProviders) {
        this.taskScheduler = taskScheduler;
        this.taskDataProviders = taskDataProviders;
    }
    
    @Override
    public void initializeTasks() {
        try {
            // Providers check
            if (taskDataProviders == null || taskDataProviders.isEmpty()) {
                log.debug("No task providers, skip init");
                return;
            }
            
            // Aggregate schedules
            List<TaskScheduleVO> allTaskSchedules = aggregateTaskSchedules();
            
            // Schedule each
            for (TaskScheduleVO task : allTaskSchedules) {
                scheduleTask(task);
            }
            
            log.info("Init complete, loaded: {}", scheduledTasks.size());
        } catch (Exception e) {
            log.error("Init failed", e);
        }
    }

    @Override
    public boolean addTask(TaskScheduleVO task) {
        try {
            if (task == null || task.getId() == null) {
                return false;
            }

            // Validate
            if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
                return false;
            }

            if (task.getTaskExecutor() == null) {
                return false;
            }

            // Remove existing if present
            if (scheduledTasks.containsKey(task.getId())) {
                log.debug("Task exists, remove old, id: {}", task.getId());
                removeTask(task.getId());
            }

            boolean ok = scheduleTask(task);
            if (ok) {
                manualTaskIds.add(task.getId());
                logActiveTaskSnapshot("addTask-success");
            } else {
                logActiveTaskSnapshot("addTask-failed");
            }

            return ok;
        } catch (Exception e) {
            log.error("Add failed, id: {}", task.getId(), e);
            return false;
        }
    }

    @Override
    public boolean removeTask(Long taskId) {
        try {
            if (taskId == null) {
                return false;
            }

            ScheduledFuture<?> future = scheduledTasks.remove(taskId);
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
                log.debug("Removed, id: {}", taskId);
                manualTaskIds.remove(taskId);
                logActiveTaskSnapshot("removeTask-success");
                return true;
            } else {
                log.debug("Not found or already cancelled, id: {}", taskId);
                logActiveTaskSnapshot("removeTask-miss");
                return false;
            }
        } catch (Exception e) {
            log.error("Remove failed, id: {}", taskId, e);
            return false;
        }
    }

    /** Schedule one task */
    @SuppressWarnings("null")
    private boolean scheduleTask(TaskScheduleVO task) {
        try {
            if (task == null) {
                log.error("Task config is null");
                return false;
            }

            if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
                log.error("Cron is empty, id: {}", task.getId());
                return false;
            }

            if (task.getTaskExecutor() == null) {
                log.error("Task executor is null, id: {}", task.getId());
                return false;
            }

            log.debug("Schedule task id: {}, desc: {}, cron: {}", 
                    task.getId(), 
                    task.getDescription() != null ? task.getDescription() : "", 
                    task.getCronExpression());

            // schedule with CronTrigger
            ScheduledFuture<?> future;
            try {
                future = taskScheduler.schedule(
                        () -> executeTaskWithFunction(task),
                        new CronTrigger(task.getCronExpression())
                );
            } catch (IllegalArgumentException e) {
                log.error("Invalid cron, id: {}, cron: {}", task.getId(), task.getCronExpression(), e);
                return false;
            }

            scheduledTasks.put(task.getId(), future);

            log.debug("Scheduled, id: {}", task.getId());
            return true;
        } catch (Exception e) {
            log.error("Schedule failed, id: {}", task != null ? task.getId() : "null", e);
            return false;
        }
    }

    /** Execute task */
    private void executeTaskWithFunction(TaskScheduleVO task) {
        try {
            if (task == null) {
                log.error("Task config is null");
                return;
            }

            log.debug("Run task id: {}, desc: {}", task.getId(), task.getDescription() != null ? task.getDescription() : "");

            // Ensure executor exists
            if (task.getTaskExecutor() == null) {
                log.error("Task executor is null, id: {}", task.getId());
                return;
            }

            // Get and run task
            Runnable taskRunnable = task.getTaskExecutor().get();
            if (taskRunnable == null) {
                log.error("Executor returned null Runnable, id: {}", task.getId());
                return;
            }

            taskRunnable.run();

            log.debug("Task done, id: {}", task.getId());
        } catch (Exception e) {
            log.error("Run failed, id: {}", task != null ? task.getId() : "null", e);
        }
    }
    
    @Override
    public void refreshTasks() {
        log.debug("Refresh tasks");
        try {
            if (taskDataProviders == null || taskDataProviders.isEmpty()) {
                log.debug("No task providers, skip refresh");
                return;
            }
            
            List<TaskScheduleVO> allTaskSchedules = aggregateTaskSchedules();

            // Current IDs
            Set<Long> currentTaskIds = new HashSet<>();

            // Apply schedules
            for (TaskScheduleVO task : allTaskSchedules) {
                Long taskId = task.getId();
                if (taskId == null) {
                    continue;
                }
                
                currentTaskIds.add(taskId);

                // Skip existing
                if (scheduledTasks.containsKey(taskId)) {
                    continue;
                }

                // Schedule
                scheduleTask(task);
            }

            currentTaskIds.addAll(manualTaskIds);

            // Remove stale
            scheduledTasks.entrySet().removeIf(entry -> {
                Long taskId = entry.getKey();
                if (!currentTaskIds.contains(taskId)) {
                    ScheduledFuture<?> future = entry.getValue();
                    if (future != null && !future.isCancelled()) {
                        future.cancel(true);
                        log.debug("Removed stale, id: {}", taskId);
                    }
                    return true;
                }
                return false;
            });

            log.debug("Refresh complete, active: {}", scheduledTasks.size());
        } catch (Exception e) {
            log.error("Refresh failed", e);
        }
    }

    @Override
    public void cleanInvalidTasks() {
        log.debug("Clean invalid tasks");
        try {
            if (taskDataProviders == null || taskDataProviders.isEmpty()) {
                log.debug("No task providers, skip clean");
                return;
            }
            
            List<Long> allInvalidTaskIds = aggregateInvalidTaskIds();
            
            if (allInvalidTaskIds.isEmpty()) {
                log.debug("No invalid tasks");
                return;
            }
            
            log.info("Found {} invalid tasks", allInvalidTaskIds.size());
            
            // Remove them
            for (Long taskId : allInvalidTaskIds) {
                if (taskId == null) {
                    continue;
                }
                ScheduledFuture<?> future = scheduledTasks.remove(taskId);
                if (future != null && !future.isCancelled()) {
                    future.cancel(true);
                    log.debug("Removed invalid, id: {}", taskId);
                }
            }
            
            log.info("Clean complete, active: {}", scheduledTasks.size());
        } catch (Exception e) {
            log.error("Clean failed", e);
        }
    }

    @Override
    public void stopAllTasks() {
        scheduledTasks.forEach((id, future) -> {
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
            }
        });
        scheduledTasks.clear();
        manualTaskIds.clear();
    }

    @Override
    public int getActiveTaskCount() {
        return scheduledTasks.size();
    }

    @Override
    public void destroy() {
        stopAllTasks();
    }

    private List<TaskScheduleVO> aggregateTaskSchedules() {
        List<TaskScheduleVO> allTaskSchedules = new ArrayList<>();
        if (taskDataProviders == null || taskDataProviders.isEmpty()) {
            return allTaskSchedules;
        }
        
        for (ITaskDataProvider provider : taskDataProviders) {
            if (provider == null) {
                continue;
            }
            List<TaskScheduleVO> taskSchedules = provider.queryAllValidTaskSchedule();
            if (taskSchedules != null) {
                allTaskSchedules.addAll(taskSchedules);
            }
        }
        return allTaskSchedules;
    }

    private List<Long> aggregateInvalidTaskIds() {
        List<Long> allInvalidTaskIds = new ArrayList<>();
        if (taskDataProviders == null || taskDataProviders.isEmpty()) {
            return allInvalidTaskIds;
        }
        
        for (ITaskDataProvider provider : taskDataProviders) {
            if (provider == null) {
                continue;
            }
            List<Long> invalidTaskIds = provider.queryAllInvalidTaskScheduleIds();
            if (invalidTaskIds != null) {
                // filter out null
                for (Long taskId : invalidTaskIds) {
                    if (taskId != null) {
                        allInvalidTaskIds.add(taskId);
                    }
                }
            }
        }
        return allInvalidTaskIds;
    }

    private void logActiveTaskSnapshot(String scene) {
        try {
            log.debug("Snapshot[{}] active: {}", scene, scheduledTasks.size());
            if (!scheduledTasks.isEmpty()) {
                log.debug("Snapshot[{}] ids: {}", scene, scheduledTasks.keySet());
            }
        } catch (Exception ignored) {
        }
    }
}