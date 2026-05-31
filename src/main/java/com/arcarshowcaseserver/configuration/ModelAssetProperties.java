package com.arcarshowcaseserver.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ModelAssetProperties {

    @Value("${app.models.static-base-url:}")
    private String staticBaseUrl;

    @Value("${app.models.static-version:}")
    private String staticVersion;

    @Value("${app.models.generated-base-url:}")
    private String generatedBaseUrl;

    @Value("${app.models.generated-version:}")
    private String generatedVersion;

    public String buildStaticModelUrl(String filename) {
        String base = normalizeBaseUrl(staticBaseUrl, "/api/static/models");
        return appendVersion(base + "/" + filename, staticVersion);
    }

    public String buildGeneratedModelUrl(String filename) {
        String base = normalizeBaseUrl(generatedBaseUrl, "/api/models");
        return appendVersion(base + "/" + filename, generatedVersion);
    }

    private String normalizeBaseUrl(String value, String fallback) {
        String base = value == null ? "" : value.trim();
        if (base.isBlank()) {
            base = fallback;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String appendVersion(String url, String version) {
        if (version == null || version.isBlank()) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "v=" + version.trim();
    }
}
