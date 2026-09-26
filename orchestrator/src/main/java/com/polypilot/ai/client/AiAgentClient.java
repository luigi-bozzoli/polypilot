package com.polypilot.ai.client;

import com.polypilot.ai.dto.AnalyzeAccepted;
import com.polypilot.ai.dto.AnalyzeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Synchronous HTTP client to trigger the ai-agent analysis pipeline. The call
 * itself is blocking, but only to get a fast "accepted" acknowledgement — the
 * actual LangGraph run happens on ai-agent's side after it responds.
 *
 * <p>Pinned to HTTP/1.1, same reason as {@link com.polypilot.wallet.client.AuthServiceClient}:
 * the JDK HTTP client's default {@code h2c} upgrade attempt silently drops the
 * POST body against uvicorn.
 */
@Component
public class AiAgentClient {

    private final RestClient restClient;

    public AiAgentClient(@Value("${services.ai-url}") String aiAgentUrl) {
        HttpClient http11 = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(aiAgentUrl)
                .requestFactory(new JdkClientHttpRequestFactory(http11))
                .build();
    }

    public AnalyzeAccepted analyze(AnalyzeRequest request) {
        return restClient.post()
                .uri("/ai/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AnalyzeAccepted.class);
    }
}
