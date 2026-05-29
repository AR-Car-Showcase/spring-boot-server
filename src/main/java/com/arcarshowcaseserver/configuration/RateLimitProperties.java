package com.arcarshowcaseserver.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class RateLimitProperties {

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.general.capacity:120}")
    private int generalCapacity;

    @Value("${app.rate-limit.general.refill-per-minute:120}")
    private int generalRefillPerMinute;

    @Value("${app.rate-limit.auth.capacity:6}")
    private int authCapacity;

    @Value("${app.rate-limit.auth.refill-per-minute:6}")
    private int authRefillPerMinute;

    @Value("${app.rate-limit.expensive.capacity:12}")
    private int expensiveCapacity;

    @Value("${app.rate-limit.expensive.refill-per-minute:12}")
    private int expensiveRefillPerMinute;
}
