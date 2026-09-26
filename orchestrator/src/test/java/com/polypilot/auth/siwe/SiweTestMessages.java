package com.polypilot.auth.siwe;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Builds EIP-4361 message strings for the SIWE tests. */
final class SiweTestMessages {

    static final String ADDRESS = "0xf2D4207201247F0026342Db44dFC912cB9F84A1F";
    static final String DOMAIN = "localhost:5173";
    static final String URI = "http://localhost:5173";

    private SiweTestMessages() {
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private String domain = DOMAIN;
        private String address = ADDRESS;
        private String uri = URI;
        private String version = "1";
        private long chainId = 1;
        private String nonce = "abc12345def";
        private String issuedAt = OffsetDateTime.now(ZoneOffset.UTC).toString();
        private String expirationTime; // optional
        private boolean includeVersion = true;
        private boolean includeChainId = true;
        private boolean includeUri = true;

        Builder domain(String v) { this.domain = v; return this; }
        Builder address(String v) { this.address = v; return this; }
        Builder uri(String v) { this.uri = v; return this; }
        Builder version(String v) { this.version = v; return this; }
        Builder chainId(long v) { this.chainId = v; return this; }
        Builder nonce(String v) { this.nonce = v; return this; }
        Builder issuedAt(String v) { this.issuedAt = v; return this; }
        Builder issuedAt(OffsetDateTime v) { this.issuedAt = v.toString(); return this; }
        Builder expirationTime(OffsetDateTime v) { this.expirationTime = v.toString(); return this; }
        Builder noVersion() { this.includeVersion = false; return this; }
        Builder noChainId() { this.includeChainId = false; return this; }
        Builder noUri() { this.includeUri = false; return this; }

        String build() {
            StringBuilder sb = new StringBuilder();
            sb.append(domain).append(" wants you to sign in with your Ethereum account:\n");
            sb.append(address).append("\n\n");
            sb.append("Sign in to PolyPilot\n\n");
            if (includeUri) {
                sb.append("URI: ").append(uri).append("\n");
            }
            if (includeVersion) {
                sb.append("Version: ").append(version).append("\n");
            }
            if (includeChainId) {
                sb.append("Chain ID: ").append(chainId).append("\n");
            }
            sb.append("Nonce: ").append(nonce).append("\n");
            sb.append("Issued At: ").append(issuedAt);
            if (expirationTime != null) {
                sb.append("\nExpiration Time: ").append(expirationTime);
            }
            return sb.toString();
        }
    }
}
