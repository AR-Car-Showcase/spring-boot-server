package com.arcarshowcaseserver.controller;

import com.sricharan.security.autoconfigure.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/auth/google", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class GoogleAuthConfigController {

    private final SecurityProperties securityProperties;

    @GetMapping("/config")
    public Map<String, Object> config() {
        SecurityProperties.Google google = securityProperties.getGoogle();
        List<String> clientIds = google.getClientIds();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", google.isEnabled());
        body.put("issuerUri", google.getIssuerUri());
        body.put("autoLinkByEmail", google.isAutoLinkByEmail());
        body.put("clientIds", clientIds);
        body.put("clientId", clientIds.isEmpty() ? "" : clientIds.get(0));
        return body;
    }
}
