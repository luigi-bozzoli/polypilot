package com.polypilot.auth.repository;

import com.polypilot.auth.entity.UserIdentity;
import com.polypilot.auth.enums.IdentityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    Optional<UserIdentity> findByTypeAndIdentifier(IdentityType type, String identifier);

    List<UserIdentity> findByUserId(UUID userId);

    boolean existsByTypeAndIdentifier(IdentityType type, String identifier);

}
