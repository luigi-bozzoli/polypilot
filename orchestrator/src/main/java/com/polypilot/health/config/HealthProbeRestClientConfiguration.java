package com.polypilot.health.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * A dedicated {@link RestClient} for {@code ServiceProbe} with explicit connect
 * and read timeouts (see {@link HealthProbeProperties}), so a hung downstream
 * can never stall the aggregate {@code GET /health}.
 *
 * <p>Deliberately separate from the shared application {@code restClient} bean —
 * that one is also used for Polymarket market sync and must keep its own
 * (unbounded) behaviour. Pinned to HTTP/1.1 for the same reason as
 * {@code AuthServiceClient}: uvicorn does not support the {@code h2c} upgrade.
 */
@Configuration
public class HealthProbeRestClientConfiguration {

    @Bean
    public RestClient healthProbeRestClient(HealthProbeProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
