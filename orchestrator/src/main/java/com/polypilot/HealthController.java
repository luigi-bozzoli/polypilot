package com.polypilot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${services.auth-url}")
    private String authUrl;

    @Value("${services.ai-url}")
    private String aiUrl;

    // Plain RestTemplate is fine for a hello-world probe
    private final RestTemplate rest = new RestTemplate();

    /**
     * GET /health
     *
     * Calls each downstream service and reports their status.
     * This proves all three services are wired together correctly.
     * The Docker healthcheck uses /actuator/health (from Spring Actuator).
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "orchestrator");
        result.put("status", "ok");
        result.put("auth-service", probe(authUrl + "/health"));
        result.put("ai-agent",     probe(aiUrl    + "/health"));
        return result;
    }

    private Object probe(String url) {
        try {
            //noinspection unchecked
            return rest.getForObject(url, Map.class);
        } catch (Exception e) {
            return Map.of("status", "unreachable", "error", e.getMessage());
        }
    }
}
