package com.arcarshowcaseserver.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Outbound HTTP clients. The two downstream services have very different latency
 * profiles, so they must not share a timeout budget: Blender legitimately renders
 * for minutes, while the recommendation service answers in milliseconds. A single
 * template would force the fast dependency to wait out the slow one's budget.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${blender.service.connect-timeout-seconds:15}")
    private long connectTimeoutSeconds;

    @Value("${blender.service.read-timeout-seconds:180}")
    private long readTimeoutSeconds;

    @Value("${ml.service.connect-timeout-seconds:2}")
    private long mlConnectTimeoutSeconds;

    @Value("${ml.service.read-timeout-seconds:5}")
    private long mlReadTimeoutSeconds;

    /**
     * Long-running client for model generation.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }

    /**
     * Short-fused client for the recommendation service. Recommendations are a
     * non-critical enhancement, so a slow or wedged ML service must give the
     * request thread back quickly rather than hold it for the Blender budget.
     */
    @Bean("mlServiceRestTemplate")
    public RestTemplate mlServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(mlConnectTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(mlReadTimeoutSeconds))
                .build();
    }
}
