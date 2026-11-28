package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareInvite;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareInviteDaoService {

    /** Row-locked until the transaction ends. */
    Optional<ShareInvite> findByHashForUpdate(String tokenHash);

    /** Neither accepted, revoked nor expired at {@code now}; newest first. */
    List<ShareInvite> findPendingOfOwner(UUID ownerId, Instant now);

    /** The owner's last invitation to this address, whatever became of it. */
    Optional<ShareInvite> findLatest(UUID ownerId, String email);

    ShareInvite save(ShareInvite invite);

    List<ShareInvite> saveAll(List<ShareInvite> invites);
}
