package devkit.component.dynamic.config.center.test.task;

import devkit.component.task.job.model.TaskScheduleVO;
import devkit.component.task.job.service.ITaskJobService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskJobTest {

    private final Logger log = LoggerFactory.getLogger(TaskJobTest.class);

    @Resource
    private ITaskJobService taskJobService;

    /**
     * 测试刷新任务
     */
    @Test
    public void testRefreshTasks() {
        log.info("开始测试刷新任务功能");
        
        try {
            // 刷新任务
            taskJobService.refreshTasks();
            
            // 获取当前活跃任务数量
            int activeTaskCount = taskJobService.getActiveTaskCount();
            log.info("当前活跃任务数量: {}", activeTaskCount);
            
            // 等待一段时间观察任务执行
            Thread.sleep(60000); // 等待60秒，观察任务执行情况
            
        } catch (Exception e) {
            log.error("测试刷新任务功能失败", e);
        }
        
        log.info("刷新任务功能测试完成");
    }

    /**
     * 测试清理无效任务
     */
    @Test
    public void testCleanInvalidTasks() {
        log.info("开始测试清理无效任务功能");
        
        try {
            // 先刷新任务
            taskJobService.refreshTasks();
            
            int beforeCount = taskJobService.getActiveTaskCount();
            log.info("清理前活跃任务数量: {}", beforeCount);
            
            // 清理无效任务
            taskJobService.cleanInvalidTasks();
            
            int afterCount = taskJobService.getActiveTaskCount();
            log.info("清理后活跃任务数量: {}", afterCount);
            
        } catch (Exception e) {
            log.error("测试清理无效任务功能失败", e);
        }
        
        log.info("清理无效任务功能测试完成");
    }

    /**
     * 测试停止所有任务
     */
    @Test
    public void testStopAllTasks() {
        log.info("开始测试停止所有任务功能");
        
        try {
            // 先刷新任务
            taskJobService.refreshTasks();
            
            int beforeCount = taskJobService.getActiveTaskCount();
            log.info("停止前活跃任务数量: {}", beforeCount);
            
            // 停止所有任务
            taskJobService.stopAllTasks();
            
            int afterCount = taskJobService.getActiveTaskCount();
            log.info("停止后活跃任务数量: {}", afterCount);
            
        } catch (Exception e) {
            log.error("测试停止所有任务功能失败", e);
        }
        
        log.info("停止所有任务功能测试完成");
    }

    /**
     * 测试移除任务
     */
    @Test
    public void testRemoveTask() {
        log.info("开始测试移除任务功能");
        
        try {
            // 先添加一个测试任务
            TaskScheduleVO testTask = new TaskScheduleVO();
            testTask.setId(8888L);
            testTask.setDescription("待移除的测试任务");
            testTask.setCronExpression("0/20 * * * * ?");
            testTask.setTaskParam("{\"message\":\"这个任务将被移除\"}");
            
            // 设置任务执行逻辑
            testTask.setTaskLogic(() -> {
                log.info("执行测试任务 - 任务ID: 8888, 任务参数: {}", testTask.getTaskParam());
            });

            // 添加任务
            boolean addResult = taskJobService.addTask(testTask);
            log.info("添加任务结果: {}", addResult ? "成功" : "失败");
            
            int beforeCount = taskJobService.getActiveTaskCount();
            log.info("移除前活跃任务数量: {}", beforeCount);
            
            // 等待一段时间
            Thread.sleep(30000); // 等待30秒，让任务执行一次
            
            // 移除任务
            boolean removeResult = taskJobService.removeTask(8888L);
            log.info("移除任务结果: {}", removeResult ? "成功" : "失败");
            
            int afterCount = taskJobService.getActiveTaskCount();
            log.info("移除后活跃任务数量: {}", afterCount);
            
        } catch (Exception e) {
            log.error("测试移除任务功能失败", e);
        }
        
        log.info("移除任务功能测试完成");
    }

    /**
     * 综合测试
     */
    @Test
    public void testTaskJobIntegration() {
        log.info("开始综合测试任务调度功能");

        try {
            // 1. 刷新任务
            log.info("=== 步骤1: 刷新任务 ===");
            taskJobService.refreshTasks();
            log.info("当前活跃任务数量: {}", taskJobService.getActiveTaskCount());

            // 2. 等待任务执行
            log.info("=== 步骤2: 等待任务执行 ===");
            Thread.sleep(30000); // 等待30秒，让任务执行一次

            // 3. 清理无效任务
            log.info("=== 步骤3: 清理无效任务 ===");
            taskJobService.cleanInvalidTasks();
            log.info("清理后活跃任务数量: {}", taskJobService.getActiveTaskCount());

            // 4. 再次等待观察
            log.info("=== 步骤4: 继续观察任务执行 ===");
            Thread.sleep(30000); // 再等待30秒

            // 5. 停止所有任务
            log.info("=== 步骤5: 停止所有任务 ===");
            taskJobService.stopAllTasks();
            log.info("最终活跃任务数量: {}", taskJobService.getActiveTaskCount());

        } catch (Exception e) {
            log.error("综合测试失败", e);
        }

        log.info("综合测试完成");
    }

}