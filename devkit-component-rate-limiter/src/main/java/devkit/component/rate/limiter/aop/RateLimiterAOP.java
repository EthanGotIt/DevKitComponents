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

    // Per-user rate cache, 1 minute
    private final Cache<String, RateLimiter> loginRecord = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    // Blacklist, 24 hours
    private final Cache<String, Long> blacklist = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    @Pointcut("@annotation(devkit.component.rate.limiter.types.annotations.RateLimiterAccessInterceptor)")
    public void aopPoint() {
    }

    @Around("aopPoint() && @annotation(rateLimiterAccessInterceptor)")
    public Object doRouter(ProceedingJoinPoint jp, RateLimiterAccessInterceptor rateLimiterAccessInterceptor) throws Throwable {
        // Switch: open/close
        if (StringUtils.isBlank(rateLimiterSwitch) || "close".equals(rateLimiterSwitch)) {
            log.debug("Rate limit disabled");
            return jp.proceed();
        }

        String key = rateLimiterAccessInterceptor.key();
        if (StringUtils.isBlank(key)) {
            throw new RuntimeException("annotation RateLimiter key is null！");
        }
        // Resolve key attr
        String keyAttr = getAttrValue(key, jp.getArgs());
        if (StringUtils.isBlank(keyAttr)) {
            return jp.proceed();
        }
        log.debug("Rate limit check key: {}, attr: {}, method: {}", key, keyAttr, jp.getSignature().toShortString());

        // Blacklist check
        if (!"all".equals(keyAttr) && rateLimiterAccessInterceptor.blacklistCount() != 0) {
            Long blacklistCount = blacklist.getIfPresent(keyAttr);
            if (blacklistCount != null && blacklistCount > rateLimiterAccessInterceptor.blacklistCount()) {
                log.info("Blocked: blacklisted, attr: {}", keyAttr);
                try {
                    return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
                } catch (Exception e) {
                    log.error("Fallback failed, method: {}", rateLimiterAccessInterceptor.fallbackMethod(), e);
                    throw e;
                }
            }
        }

        // Build limiter (cached)
        RateLimiter rateLimiter = loginRecord.getIfPresent(keyAttr);
        if (null == rateLimiter) {
            rateLimiter = RateLimiter.create(rateLimiterAccessInterceptor.permitsPerSecond());
            loginRecord.put(keyAttr, rateLimiter);
            log.debug("Create limiter, attr: {}, rps: {}", keyAttr, rateLimiterAccessInterceptor.permitsPerSecond());
        }

        // Try acquire
        boolean acquired = rateLimiter.tryAcquire();
        log.debug("Acquire permit, attr: {}, acquired: {}, rps: {}", keyAttr, acquired, rateLimiterAccessInterceptor.permitsPerSecond());
        
        if (!acquired) {
            log.info("Blocked: rate exceeded, attr: {}, rps: {}", keyAttr, rateLimiterAccessInterceptor.permitsPerSecond());
            
            // Update blacklist count
            if (rateLimiterAccessInterceptor.blacklistCount() != 0) {
                Long currentCount = blacklist.getIfPresent(keyAttr);
                long newCount = (currentCount == null ? 0L : currentCount) + 1L;
                blacklist.put(keyAttr, newCount);
                log.debug("Blacklist count updated, attr: {}, count: {}", keyAttr, newCount);
            }
            
            try {
                return fallbackMethodResult(jp, rateLimiterAccessInterceptor.fallbackMethod());
            } catch (Exception e) {
                log.error("Fallback failed, method: {}", rateLimiterAccessInterceptor.fallbackMethod(), e);
                throw e;
            }
        }

        log.debug("Allowed, attr: {}", keyAttr);
        // Return result
        return jp.proceed();
    }

    /** Invoke fallback when blocked */
    private Object fallbackMethodResult(JoinPoint jp, String fallbackMethod) throws Exception {
        if (jp == null || jp.getTarget() == null || jp.getSignature() == null) {
            throw new IllegalArgumentException("JoinPoint is incomplete");
        }
        
        if (fallbackMethod == null || fallbackMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Fallback method is blank");
        }
        
        Signature sig = jp.getSignature();
        if (!(sig instanceof MethodSignature)) {
            throw new IllegalArgumentException("Not a MethodSignature");
        }
        
        MethodSignature methodSignature = (MethodSignature) sig;
        Method method = jp.getTarget().getClass().getMethod(fallbackMethod, methodSignature.getParameterTypes());
        return method.invoke(jp.getThis(), jp.getArgs());
    }

    /** Get key value by attr */
    public String getAttrValue(String attr, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        
        // First arg is String
        if (args[0] != null && args[0] instanceof String) {
            String value = args[0].toString();
            log.debug("Use first String arg, attr: {}, value: {}", attr, value);
            return value;
        }
        
        // If attr is "all"
        if ("all".equals(attr)) {
            return "all";
        }
        
        // Try to read field from object args
        String filedValue = null;
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                if (StringUtils.isNotBlank(filedValue)) {
                    break;
                }
                // Use reflection to read field
                Object value = this.getValueByName(arg, attr);
                if (value != null) {
                    filedValue = String.valueOf(value);
                }
            } catch (Exception e) {
                log.debug("Read field failed, attr: {}, class: {}", attr, arg.getClass().getName(), e);
            }
        }
        
        return filedValue;
    }

    /** Get a field value */
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

    /** Get a declared field by name, including superclass */
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

