package com.polypilot.wallet;

import com.polypilot.wallet.controller.WalletController;
import com.polypilot.wallet.service.WalletService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards that the legacy wallet-challenge round-trip is actually gone — not merely
 * unused. These are deliberately structural (reflection / classloading) assertions:
 * the point is to prove the removal, so a future re-introduction fails loudly.
 */
class WalletChallengeRemovedTest {

    @Test
    void controllerExposesNoChallengeEndpoint() {
        assertThat(Arrays.stream(WalletController.class.getDeclaredMethods()).map(Method::getName))
                .noneMatch(name -> name.toLowerCase().contains("challenge"));
    }

    @Test
    void serviceExposesNoChallengeGeneration() {
        assertThat(Arrays.stream(WalletService.class.getDeclaredMethods()).map(Method::getName))
                .noneMatch(name -> name.toLowerCase().contains("challenge"));
    }

    @Test
    void serviceKeepsNoChallengeOrNonceConstants() {
        assertThat(Arrays.stream(WalletService.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("DEFAULT_NONCE", "CHALLENGE_PREFIX", "CHALLENGE_TTL");
    }

    @Test
    void challengeResponseDtoIsDeleted() {
        assertThatThrownBy(() -> Class.forName("com.polypilot.wallet.dto.ChallengeResponse"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void serviceHasNoRedisChallengeCollaborator() {
        assertThat(Arrays.stream(WalletService.class.getDeclaredFields())
                .map(f -> f.getType().getName()))
                .noneMatch(name -> name.contains("Redis"));
    }
}
