package devkit.component.test;

import devkit.component.dynamic.config.center.domain.model.valobj.AttributeVO;
import devkit.component.dynamic.config.center.types.annotations.DCCValue;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RTopic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest()
public class ApiTest {

    @DCCValue("downgradeSwitch:0")
    private String downgradeSwitch;

    @Resource
    private RTopic dynamicConfigCenterRedisTopic;

    @Test
    public void test_get() throws InterruptedException {
        log.info("result:{}", downgradeSwitch);
    }

    @Test
    public void test_publish() throws InterruptedException {
        // publish
        dynamicConfigCenterRedisTopic.publish(new AttributeVO("downgradeSwitch", "4"));

        new CountDownLatch(1).await();
    }

}
