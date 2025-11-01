package devkit.component.rate.limiter.aop;

import devkit.component.dynamic.config.center.types.annotations.DCCValue;
import devkit.component.rate.limiter.types.annotations.RateLimiterAccessInterceptor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;


@Aspect
public class RateLimiterAOP {

    private final Logger log = LoggerFactory.getLogger(RateLimiterAOP.class);

    @DCCValue("rateLimiterSwitch:open")
    private String rateLimiterSwitch = "open";

    // 个人限频记录1分钟
    private final Cache<String, RateLimiter> loginRecord = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    // 个人限频黑名单24h - 分布式业务场景，可以记录到 Redis 中
    private final Cache<String, Long> blacklist = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Pointcut("@annotation(devkit.component.rate.limiter.types.annotations.RateLimiterAccessInterceptor)")
    public void aopPoint() {
    }

    @Around("aopPoint() && @annotation(rateLimiterAccessInterceptor)")
    public Object doRouter(ProceedingJoinPoint jp, RateLimiterAccessInterceptor rateLimiterAccessInterceptor) throws Throwable {
        // 0. 限流开关【open 开启、close 关闭】关闭后，不会走限流策略
        if (StringUtils.isBlank(rateLimiterSwitch) || "close".equals(rateLimiterSwitch)) {
            log.debug("限流开关已关闭，跳过限流检查");
            return jp.proceed();
        }

        String key = rateLimiterAccessInterceptor.key();
        if (StringUtils.isBlank(key)) {
            throw new RuntimeException("annotation RateLimiter key is null！");
        }
        // 获取拦截字段
        String keyAttr = getAttrValue(key, jp.getArgs());
        if (StringUtils.isBlank(keyAttr)) {
            return jp.proceed();
        }
        log.debug("限流检查，key: {}, keyAttr: {}, method: {}", key, keyAttr, jp.getSignature().toShortString());

        // 黑名单拦截
        if (!"all".equals(keyAttr) && rateLimiterAccessInterceptor.blacklistCount() != 0) {
            Long blacklistCount = blacklist.getIfPresent(keyAttr);
            if (blacklistCount != null && blacklistCount > rateLimiterAccessInterceptor.blacklistCount()) {
                log.info("限流拦截：黑名单用户，keyAttr: {}, 拦截时长: 24h", keyAttr);
                try {
                    return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
                } catch (Exception e) {
                    log.error("调用限流回调方法失败，fallbackMethod: {}", rateLimiterAccessInterceptor.fallbackMethod(), e);
                    throw e;
                }
            }
        }

        // 获取限流 -> Guava 缓存1分钟
        RateLimiter rateLimiter = loginRecord.getIfPresent(keyAttr);
        if (null == rateLimiter) {
            rateLimiter = RateLimiter.create(rateLimiterAccessInterceptor.permitsPerSecond());
            loginRecord.put(keyAttr, rateLimiter);
            log.debug("创建限流器，keyAttr: {}, permitsPerSecond: {}", keyAttr, rateLimiterAccessInterceptor.permitsPerSecond());
        }

        // 限流拦截
        boolean acquired = rateLimiter.tryAcquire();
        log.debug("限流器尝试获取许可，keyAttr: {}, acquired: {}, permitsPerSecond: {}", keyAttr, acquired, rateLimiterAccessInterceptor.permitsPerSecond());
        
        if (!acquired) {
            log.info("限流拦截：请求超频，keyAttr: {}, 速率限制: {} 次/秒", keyAttr, rateLimiterAccessInterceptor.permitsPerSecond());
            
            // 记录黑名单次数
            if (rateLimiterAccessInterceptor.blacklistCount() != 0) {
                Long currentCount = blacklist.getIfPresent(keyAttr);
                long newCount = (currentCount == null ? 0L : currentCount) + 1L;
                blacklist.put(keyAttr, newCount);
                log.debug("更新黑名单计数，keyAttr: {}, count: {}", keyAttr, newCount);
            }
            
            try {
                return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
            } catch (Exception e) {
                log.error("调用限流回调方法失败，fallbackMethod: {}", rateLimiterAccessInterceptor.fallbackMethod(), e);
                throw e;
            }
        }

        log.debug("限流检查通过，允许执行，keyAttr: {}", keyAttr);
        // 返回结果
        return jp.proceed();
    }

    /**
     * 调用用户配置的回调方法，当拦截后，返回回调结果。
     */
    private Object fallbackMethodResult(JoinPoint jp, String fallbackMethod) throws Exception {
        if (jp == null || jp.getTarget() == null || jp.getSignature() == null) {
            throw new IllegalArgumentException("JoinPoint 参数不完整");
        }
        
        if (fallbackMethod == null || fallbackMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("回调方法名不能为空");
        }
        
        Signature sig = jp.getSignature();
        if (!(sig instanceof MethodSignature)) {
            throw new IllegalArgumentException("无法获取方法签名");
        }
        
        MethodSignature methodSignature = (MethodSignature) sig;
        Method method = jp.getTarget().getClass().getMethod(fallbackMethod, methodSignature.getParameterTypes());
        return method.invoke(jp.getThis(), jp.getArgs());
    }

    /**
     * 实际根据自身业务调整，主要是为了获取通过某个值做拦截
     */
    public String getAttrValue(String attr, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        
        // 如果第一个参数是 String 类型（常见场景：Controller 方法参数是 String userId）
        if (args[0] != null && args[0] instanceof String) {
            String value = args[0].toString();
            log.debug("从第一个String参数获取值，attr: {}, value: {}", attr, value);
            return value;
        }
        
        // 如果 attr 是 "all"，返回 "all"
        if ("all".equals(attr)) {
            return "all";
        }
        
        // 尝试从对象参数中获取属性值
        String filedValue = null;
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                if (StringUtils.isNotBlank(filedValue)) {
                    break;
                }
                // fix: 使用lombok时，uId这种字段的get方法与idea生成的get方法不同，会导致获取不到属性值，改成反射获取解决
                Object value = this.getValueByName(arg, attr);
                if (value != null) {
                    filedValue = String.valueOf(value);
                }
            } catch (Exception e) {
                log.debug("从对象中获取属性值失败，attr: {}, argClass: {}", attr, arg.getClass().getName(), e);
            }
        }
        
        return filedValue;
    }

    /**
     * 获取对象的特定属性值
     *
     * @param item 对象
     * @param name 属性名
     * @return 属性值
     * @author tang
     */
    private Object getValueByName(Object item, String name) {
        try {
            Field field = getFieldByName(item, name);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object o = field.get(item);
            field.setAccessible(false);
            return o;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 根据名称获取方法，该方法同时兼顾继承类获取父类的属性
     *
     * @param item 对象
     * @param name 属性名
     * @return 该属性对应方法
     * @author tang
     */
    private Field getFieldByName(Object item, String name) {
        try {
            Field field;
            try {
                field = item.getClass().getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                Class<?> superclass = item.getClass().getSuperclass();
                if (superclass == null || superclass == Object.class) {
                    return null;
                }
                field = superclass.getDeclaredField(name);
            }
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

}

