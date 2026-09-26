package com.polypilot.auth.enums;

/**
 * The kind of sign-in method a {@link com.polypilot.auth.entity.UserIdentity} represents.
 * Persisted as a string via {@code @Enumerated(EnumType.STRING)} into
 * {@code user_identities.type} (VARCHAR(32)).
 */
public enum IdentityType {

    /** {@code identifier} is the lower-cased email; {@code secret} is a BCrypt hash. */
    PASSWORD,

    /** {@code identifier} is the lower-cased {@code 0x…} address; {@code secret} is null. */
    ETH_WALLET
}
