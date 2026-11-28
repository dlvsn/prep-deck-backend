package denys.mazurenko.prepdeckbackend.persistence.share.repository;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareInvite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareInviteRepository extends JpaRepository<ShareInvite, UUID> {

    /** Row-locked: two acceptances of one letter queue up, and the second finds it used. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ShareInvite i where i.tokenHash = :hash")
    Optional<ShareInvite> findByHashForUpdate(@Param("hash") String hash);

    @Query("""
            select i from ShareInvite i
            where i.ownerId = :ownerId
              and i.acceptedAt is null
              and i.revokedAt is null
              and i.expiresAt > :now
            order by i.createdAt desc
            """)
    List<ShareInvite> findPendingOfOwner(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

    /** The owner's last letter to this address, for the resend cooldown. */
    Optional<ShareInvite> findFirstByOwnerIdAndEmailOrderByCreatedAtDesc(UUID ownerId, String email);
}
