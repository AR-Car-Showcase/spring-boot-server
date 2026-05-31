package com.arcarshowcaseserver.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class RateLimitProperties {

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.model.capacity:8}")
    private int modelCapacity;

    @Value("${app.rate-limit.model.refill-per-minute:8}")
    private int modelRefillPerMinute;
}
