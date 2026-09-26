package com.polypilot.auth.siwe;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SiweMessageTest {

    @Test
    void parsesAllValidatedFields() {
        OffsetDateTime issued = OffsetDateTime.of(2026, 8, 30, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime expires = issued.plusMinutes(5);

        SiweMessage msg = SiweMessage.parse(SiweTestMessages.builder()
                .chainId(137)
                .nonce("Nonce123abc")
                .issuedAt(issued)
                .expirationTime(expires)
                .build());

        assertThat(msg.getDomain()).isEqualTo("localhost:5173");
        assertThat(msg.getAddress()).isEqualTo(SiweTestMessages.ADDRESS);
        assertThat(msg.getUri()).isEqualTo("http://localhost:5173");
        assertThat(msg.getChainId()).isEqualTo(137);
        assertThat(msg.getNonce()).isEqualTo("Nonce123abc");
        assertThat(msg.getIssuedAt()).isEqualTo(issued);
        assertThat(msg.getExpirationTime()).isEqualTo(expires);
    }

    @Test
    void expirationTimeIsOptional() {
        SiweMessage msg = SiweMessage.parse(SiweTestMessages.builder().build());
        assertThat(msg.getExpirationTime()).isNull();
    }

    @Test
    void rejectsEmptyMessage() {
        assertThatThrownBy(() -> SiweMessage.parse("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedPreamble() {
        assertThatThrownBy(() -> SiweMessage.parse("hello world\n" + SiweTestMessages.ADDRESS + "\n\nURI: x\n"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBadAddress() {
        String bad = SiweTestMessages.builder().build()
                .replace(SiweTestMessages.ADDRESS, "0x1234");
        assertThatThrownBy(() -> SiweMessage.parse(bad))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedVersion() {
        assertThatThrownBy(() -> SiweMessage.parse(SiweTestMessages.builder().version("2").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingVersion() {
        assertThatThrownBy(() -> SiweMessage.parse(SiweTestMessages.builder().noVersion().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingChainId() {
        assertThatThrownBy(() -> SiweMessage.parse(SiweTestMessages.builder().noChainId().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingUri() {
        assertThatThrownBy(() -> SiweMessage.parse(SiweTestMessages.builder().noUri().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsShortNonce() {
        assertThatThrownBy(() -> SiweMessage.parse(SiweTestMessages.builder().nonce("short").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonAlphanumericNonce() {
        assertThatThrownBy(() -> SiweMessage.parse(SiweTestMessages.builder().nonce("abc-123-def").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonIsoIssuedAt() {
        assertThatThrownBy(() -> SiweMessage.parse(SiweTestMessages.builder().issuedAt("last tuesday").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
