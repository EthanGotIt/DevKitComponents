package devkit.component.dynamic.config.center.domain.service;

import devkit.component.dynamic.config.center.domain.model.valobj.AttributeVO;

public interface IDynamicConfigCenterService {

    Object proxyObject(Object bean);

    void adjustAttributeValue(AttributeVO attributeVO);

}
