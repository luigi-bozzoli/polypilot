package com.polypilot.wallet.client;

import com.polypilot.auth.siwe.dto.SiweVerifyRequest;
import com.polypilot.auth.siwe.dto.SiweVerifyResponse;
import com.polypilot.wallet.dto.L2Credentials;
import com.polypilot.wallet.dto.WalletConnectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Synchronous HTTP client for the Python auth-service.
 * Intentionally blocking — the orchestrator must wait for the result before it
 * can store credentials or complete a login.
 *
 * <p>Pinned to HTTP/1.1: the JDK HTTP client otherwise negotiates an {@code h2c}
 * (HTTP/2 cleartext) upgrade that uvicorn does not support, and the POST body is
 * dropped on the upgrade attempt.
 */
@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(@Value("${services.auth-url}") String authServiceUrl) {
        HttpClient http11 = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .requestFactory(new JdkClientHttpRequestFactory(http11))
                .build();
    }

    public L2Credentials verifyAndDerive(WalletConnectRequest request) {
        return restClient.post()
                .uri("/auth/verify-and-derive")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(L2Credentials.class);
    }

    /**
     * Recover the Ethereum address that signed a SIWE (EIP-4361) message.
     * The auth-service does signature recovery only — SIWE policy (nonce, domain,
     * expiry, chain id, user lookup) is validated by the orchestrator.
     * A malformed/unrecoverable signature yields a 4xx, which surfaces here as a
     * {@code RestClientResponseException}.
     */
    public SiweVerifyResponse verifySiwe(String message, String signature) {
        return restClient.post()
                .uri("/auth/siwe/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SiweVerifyRequest(message, signature))
                .retrieve()
                .body(SiweVerifyResponse.class);
    }
}