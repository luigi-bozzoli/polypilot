package com.polypilot.auth.entity;

import com.polypilot.auth.enums.IdentityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single sign-in method attached to a {@link User}.
 * <ul>
 *   <li>{@link IdentityType#PASSWORD} — {@code identifier} is the lower-cased email,
 *       {@code secret} is a BCrypt hash.</li>
 *   <li>{@link IdentityType#ETH_WALLET} — {@code identifier} is the lower-cased
 *       {@code 0x…} address, {@code secret} is {@code null} (the SIWE signature
 *       is the proof).</li>
 * </ul>
 * {@code (type, identifier)} is unique across all users.
 */
@Entity
@Table(name = "user_identities")
@NoArgsConstructor
@Getter
@Setter
public class UserIdentity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdentityType type;

    @Column(nullable = false)
    private String identifier;

    /** BCrypt hash for PASSWORD identities; null for wallet identities. */
    @Column
    private String secret;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

}
