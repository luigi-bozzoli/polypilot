package com.polypilot.auth.siwe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiweNonceServiceTest {

    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOps;

    SiweNonceService service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        service = new SiweNonceService(redis);
    }

    @Test
    void issueStoresAlphanumericNonceUnderPrefixWithFiveMinuteTtl() {
        String nonce = service.issue();

        assertThat(nonce).hasSizeGreaterThanOrEqualTo(8);
        assertThat(nonce).matches("[A-Za-z0-9]+");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(key.capture(), eq("1"), ttl.capture());

        assertThat(key.getValue()).isEqualTo("siwe-nonce:" + nonce);
        assertThat(ttl.getValue()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void issueProducesDistinctNonces() {
        assertThat(service.issue()).isNotEqualTo(service.issue());
    }

    @Test
    void existsChecksPrefixedKey() {
        when(redis.hasKey("siwe-nonce:abc")).thenReturn(true);
        assertThat(service.exists("abc")).isTrue();
        assertThat(service.exists(null)).isFalse();
    }

    @Test
    void consumeDeletesNonceAndReturnsTrueOnce() {
        when(valueOps.getAndDelete("siwe-nonce:live")).thenReturn("1", (String) null);

        assertThat(service.consume("live")).isTrue();   // first call removed it
        assertThat(service.consume("live")).isFalse();  // replay finds nothing

        verify(valueOps, org.mockito.Mockito.times(2)).getAndDelete("siwe-nonce:live");
    }

    @Test
    void consumeNullIsFalse() {
        assertThat(service.consume(null)).isFalse();
    }
}
