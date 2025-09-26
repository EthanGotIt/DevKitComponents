package devkit.component.dynamic.config.center.config;

import devkit.component.dynamic.config.center.types.common.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devkit.component.config", ignoreInvalidFields = true)
public class DynamicConfigCenterAutoProperties {

    private String system;

    public String getKey(String attributeName) {
        return this.system + Constants.LINE + attributeName;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

}
