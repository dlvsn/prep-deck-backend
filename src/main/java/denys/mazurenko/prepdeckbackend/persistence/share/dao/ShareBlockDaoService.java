package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import java.util.UUID;

public interface ShareBlockDaoService {

    boolean isBlocked(UUID ownerId, UUID userId);

    /** Does nothing when the user is blocked already. */
    void block(UUID ownerId, UUID userId);

    void unblock(UUID ownerId, UUID userId);
}
