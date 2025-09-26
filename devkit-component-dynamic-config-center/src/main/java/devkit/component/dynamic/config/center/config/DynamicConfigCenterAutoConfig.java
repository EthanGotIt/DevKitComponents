package devkit.component.dynamic.config.center.config;

import devkit.component.dynamic.config.center.domain.service.IDynamicConfigCenterService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class DynamicConfigCenterAutoConfig implements BeanPostProcessor {

    private final IDynamicConfigCenterService dynamicConfigCenterService;

    public DynamicConfigCenterAutoConfig(IDynamicConfigCenterService dynamicConfigCenterService) {
        this.dynamicConfigCenterService = dynamicConfigCenterService;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        return dynamicConfigCenterService.proxyObject(bean);
    }

}
