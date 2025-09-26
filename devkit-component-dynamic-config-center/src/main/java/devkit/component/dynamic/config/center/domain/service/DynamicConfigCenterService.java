package devkit.component.dynamic.config.center.domain.service;

import devkit.component.dynamic.config.center.config.DynamicConfigCenterAutoProperties;
import devkit.component.dynamic.config.center.domain.model.valobj.AttributeVO;
import devkit.component.dynamic.config.center.types.annotations.DCCValue;
import devkit.component.dynamic.config.center.types.common.Constants;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicConfigCenterService implements IDynamicConfigCenterService {

    private final Logger log = LoggerFactory.getLogger(DynamicConfigCenterService.class);

    private final DynamicConfigCenterAutoProperties properties;

    private final RedissonClient redissonClient;

    private final Map<String, Object> dccBeanGroup = new ConcurrentHashMap<>();

    public DynamicConfigCenterService(DynamicConfigCenterAutoProperties properties, RedissonClient redissonClient) {
        this.properties = properties;
        this.redissonClient = redissonClient;
    }

    @Override
    public Object proxyObject(Object bean) {
        Class<?> targetBeanClass = bean.getClass();
        Object targetBeanObject = bean;

        if (AopUtils.isAopProxy(bean)) {
            targetBeanClass = AopUtils.getTargetClass(bean);
            targetBeanObject = AopProxyUtils.getSingletonTarget(bean);
        }

        Field[] fields = targetBeanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(DCCValue.class)) {
                continue;
            }

            DCCValue dccValue = field.getAnnotation(DCCValue.class);

            String value = dccValue.value();
            if (StringUtils.isBlank(value)) {
                throw new RuntimeException(field.getName() + " @DCCValue is not config value config case 「isSwitch/isSwitch:1」");
            }

            String[] splits = value.split(Constants.SYMBOL_COLON);
            String key = properties.getKey(splits[0].trim());

            String defaultValue = splits.length == 2 ? splits[1] : null;

            String setValue = defaultValue;

            try {
                if (StringUtils.isBlank(defaultValue)) {
                    throw new RuntimeException("dcc config error " + key + " is not null - please set default value to config");
                }

                RBucket<String> bucket = redissonClient.getBucket(key);
                boolean exists = bucket.isExists();
                if (!exists) {
                    bucket.set(defaultValue);
                } else {
                    setValue = bucket.get();
                }

                field.setAccessible(true);
                field.set(targetBeanObject, setValue);
                field.setAccessible(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            dccBeanGroup.put(key, targetBeanObject);
        }

        return bean;
    }

    @Override
    public void adjustAttributeValue(AttributeVO attributeVO) {
        String key = properties.getKey(attributeVO.getAttribute());
        String value = attributeVO.getValue();

        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean exists = bucket.isExists();
        if (!exists) return;
        bucket.set(attributeVO.getValue());

        Object objBean = dccBeanGroup.get(key);
        if (null == objBean) return;

        Class<?> objBeanClass = objBean.getClass();
        if (AopUtils.isAopProxy(objBean)) {
            objBeanClass = AopUtils.getTargetClass(objBean);
        }

        try {
            Field field = objBeanClass.getDeclaredField(attributeVO.getAttribute());
            field.setAccessible(true);
            field.set(objBean, value);
            field.setAccessible(false);

            log.info("DCC KEY and VALUE {} {}", key, value);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
