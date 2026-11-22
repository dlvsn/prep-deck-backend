package denys.mazurenko.prepdeckbackend.persistence.token.dao;

import denys.mazurenko.prepdeckbackend.persistence.token.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenDaoService {

    /** Locks the row until the caller's transaction ends; call it inside one. */
    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    RefreshToken save(RefreshToken token);

    /** Revokes every still-active token of the family; returns how many. */
    int revokeFamily(UUID familyId, Instant now);
}
