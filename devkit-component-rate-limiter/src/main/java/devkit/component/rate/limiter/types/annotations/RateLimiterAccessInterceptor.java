package devkit.component.rate.limiter.types.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface RateLimiterAccessInterceptor {

    /** Key to identify subject; default "all" */
    String key() default "all";

    /** Requests per second */
    double permitsPerSecond();

    /** Blacklist threshold; 0 disables */
    double blacklistCount() default 0;

    /** Fallback method name */
    String fallbackMethod();

}
