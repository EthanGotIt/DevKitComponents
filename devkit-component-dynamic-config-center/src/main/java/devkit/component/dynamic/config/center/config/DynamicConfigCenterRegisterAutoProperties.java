package devkit.component.dynamic.config.center.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devkit.component.config.register", ignoreInvalidFields = true)
public class DynamicConfigCenterRegisterAutoProperties {

    /** redis host */
    private String host;
    /** redis port */
    private int port;
    /** password */
    private String password;
    /** pool size (default 64) */
    private int poolSize = 64;
    /** min idle (default 10) */
    private int minIdleSize = 10;
    /** idle timeout (ms, default 10000) */
    private int idleTimeout = 10000;
    /** connect timeout (ms, default 10000) */
    private int connectTimeout = 10000;
    /** retry attempts (default 3) */
    private int retryAttempts = 3;
    /** retry interval (ms, default 1000) */
    private int retryInterval = 1000;
    /** ping interval (ms, default 0) */
    private int pingInterval = 0;
    /** keep alive (default true) */
    private boolean keepAlive = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMinIdleSize() {
        return minIdleSize;
    }

    public void setMinIdleSize(int minIdleSize) {
        this.minIdleSize = minIdleSize;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

}
