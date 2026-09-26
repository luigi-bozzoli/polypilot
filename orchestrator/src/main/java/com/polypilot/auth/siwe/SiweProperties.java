package com.polypilot.auth.siwe;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * SIWE (EIP-4361) policy the orchestrator enforces on inbound login messages.
 * Bound from the {@code siwe.*} block in {@code application.yml} /
 * {@code application-local.yml}.
 */
@ConfigurationProperties(prefix = "siwe")
@Getter
@Setter
public class SiweProperties {

    /** Expected {@code domain} field — the authority (host[:port]) of the dApp. */
    private String domain;

    /** Expected {@code URI} field — the full origin the sign-in request came from. */
    private String uri;

    /** Chain IDs a SIWE message is allowed to declare. */
    private List<Long> allowedChainIds = new ArrayList<>();
}
