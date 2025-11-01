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

    /**
     * 任务ID与任务执行器的映射，用于记录已添加的任务
     */
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * 新的构造函数，不依赖ITaskExecutor
     */
    public TaskJobService(TaskScheduler taskScheduler,
                         List<ITaskDataProvider> taskDataProviders) {
        this.taskScheduler = taskScheduler;
        this.taskDataProviders = taskDataProviders;
    }
    
    @Override
    public void initializeTasks() {
        try {
            // 检查数据提供者列表是否为空
            if (taskDataProviders == null || taskDataProviders.isEmpty()) {
                log.debug("没有可用的任务数据提供者，跳过初始化");
                return;
            }
            
            // 聚合所有数据提供者的任务调度配置
            List<TaskScheduleVO> allTaskSchedules = aggregateTaskSchedules();
            
            // 处理每个任务调度配置
            for (TaskScheduleVO task : allTaskSchedules) {
                // 创建并调度新任务
                scheduleTask(task);
            }
            
            log.info("任务调度配置初始化完成，加载任务数: {}", scheduledTasks.size());
        } catch (Exception e) {
            log.error("任务调度配置初始化失败", e);
        }
    }

    @Override
    public boolean addTask(TaskScheduleVO task) {
        try {
            if (task == null || task.getId() == null) {
                return false;
            }

            // 验证任务配置完整性
            if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
                return false;
            }

            if (task.getTaskExecutor() == null) {
                return false;
            }

            // 如果任务已存在，先移除旧任务
            if (scheduledTasks.containsKey(task.getId())) {
                log.debug("任务已存在，先移除旧任务，taskId: {}", task.getId());
                removeTask(task.getId());
            }

            // 调度新任务
            scheduleTask(task);

            return true;
        } catch (Exception e) {
            log.error("添加任务失败，taskId: {}", task.getId(), e);
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
                log.debug("任务移除成功，taskId: {}", taskId);
                return true;
            } else {
                log.debug("任务不存在或已取消，taskId: {}", taskId);
                return false;
            }
        } catch (Exception e) {
            log.error("移除任务失败，taskId: {}", taskId, e);
            return false;
        }
    }

    /**
     * 调度单个任务
     */
    private void scheduleTask(TaskScheduleVO task) {
        try {
            if (task == null) {
                log.error("任务配置为空，无法调度");
                return;
            }

            if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
                log.error("任务Cron表达式为空，无法调度，taskId: {}", task.getId());
                return;
            }

            if (task.getTaskExecutor() == null) {
                log.error("任务执行器为空，无法调度，taskId: {}", task.getId());
                return;
            }

            log.debug("调度任务，taskId: {}, description: {}, cron: {}", 
                    task.getId(), 
                    task.getDescription() != null ? task.getDescription() : "", 
                    task.getCronExpression());

            // 使用新的函数式编程方式
            ScheduledFuture<?> future;
            try {
                future = taskScheduler.schedule(
                        () -> executeTaskWithFunction(task),
                        new CronTrigger(task.getCronExpression())
                );
            } catch (IllegalArgumentException e) {
                log.error("Cron表达式格式错误，无法调度任务，taskId: {}, cron: {}", task.getId(), task.getCronExpression(), e);
                return;
            }

            scheduledTasks.put(task.getId(), future);

            log.debug("任务调度成功，taskId: {}", task.getId());
        } catch (Exception e) {
            log.error("调度任务失败，taskId: {}", task != null ? task.getId() : "null", e);
        }
    }

    /**
     * 使用函数式编程方式执行任务
     */
    private void executeTaskWithFunction(TaskScheduleVO task) {
        try {
            if (task == null) {
                log.error("任务配置为空，无法执行");
                return;
            }

            log.debug("执行任务，taskId: {}, description: {}", task.getId(), task.getDescription() != null ? task.getDescription() : "");

            // 检查任务执行器是否存在
            if (task.getTaskExecutor() == null) {
                log.error("任务执行器为空，无法执行，taskId: {}", task.getId());
                return;
            }

            // 获取并执行任务
            Runnable taskRunnable = task.getTaskExecutor().get();
            if (taskRunnable == null) {
                log.error("任务执行器返回的Runnable为空，无法执行，taskId: {}", task.getId());
                return;
            }

            taskRunnable.run();

            log.debug("任务执行完成，taskId: {}", task.getId());
        } catch (Exception e) {
            log.error("执行任务失败，taskId: {}", task != null ? task.getId() : "null", e);
        }
    }
    
    @Override
    public void refreshTasks() {
        log.debug("刷新任务调度配置");
        try {
            // 检查数据提供者列表是否为空
            if (taskDataProviders == null || taskDataProviders.isEmpty()) {
                log.debug("没有可用的任务数据提供者，跳过刷新");
                return;
            }
            
            // 聚合所有数据提供者的任务调度配置
            List<TaskScheduleVO> allTaskSchedules = aggregateTaskSchedules();

            // 记录当前配置中的任务ID
            Set<Long> currentTaskIds = new HashSet<>();

            // 处理每个任务调度配置
            for (TaskScheduleVO task : allTaskSchedules) {
                Long taskId = task.getId();
                if (taskId == null) {
                    continue;
                }
                
                currentTaskIds.add(taskId);

                // 如果任务已经存在，则跳过
                if (scheduledTasks.containsKey(taskId)) {
                    continue;
                }

                // 创建并调度新任务
                scheduleTask(task);
            }

            // 移除已不存在的任务
            scheduledTasks.entrySet().removeIf(entry -> {
                Long taskId = entry.getKey();
                if (!currentTaskIds.contains(taskId)) {
                    ScheduledFuture<?> future = entry.getValue();
                    if (future != null && !future.isCancelled()) {
                        future.cancel(true);
                        log.debug("移除过期任务，taskId: {}", taskId);
                    }
                    return true;
                }
                return false;
            });

            log.debug("任务调度配置刷新完成，活跃任务数: {}", scheduledTasks.size());
        } catch (Exception e) {
            log.error("刷新任务调度配置失败", e);
        }
    }

    @Override
    public void cleanInvalidTasks() {
        log.debug("清理无效任务");
        try {
            // 检查数据提供者列表是否为空
            if (taskDataProviders == null || taskDataProviders.isEmpty()) {
                log.debug("没有可用的任务数据提供者，跳过清理无效任务");
                return;
            }
            
            // 聚合所有数据提供者的无效任务ID
            List<Long> allInvalidTaskIds = aggregateInvalidTaskIds();
            
            if (allInvalidTaskIds.isEmpty()) {
                log.debug("没有发现无效的任务需要清理");
                return;
            }
            
            log.info("发现 {} 个无效任务需要清理", allInvalidTaskIds.size());
            
            // 从调度器中移除这些任务
            for (Long taskId : allInvalidTaskIds) {
                if (taskId == null) {
                    continue;
                }
                ScheduledFuture<?> future = scheduledTasks.remove(taskId);
                if (future != null && !future.isCancelled()) {
                    future.cancel(true);
                    log.debug("移除无效任务，taskId: {}", taskId);
                }
            }
            
            log.info("无效任务清理完成，活跃任务数: {}", scheduledTasks.size());
        } catch (Exception e) {
            log.error("清理无效任务失败", e);
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
                // 过滤掉 null 值
                for (Long taskId : invalidTaskIds) {
                    if (taskId != null) {
                        allInvalidTaskIds.add(taskId);
                    }
                }
            }
        }
        return allInvalidTaskIds;
    }

}