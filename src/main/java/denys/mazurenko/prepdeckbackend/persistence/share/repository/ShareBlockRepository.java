package denys.mazurenko.prepdeckbackend.persistence.share.repository;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareBlock;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareBlockKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ShareBlockRepository extends JpaRepository<ShareBlock, ShareBlockKey> {

    boolean existsByOwnerIdAndUserId(UUID ownerId, UUID userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into prep_deck.share_blocks (owner_id, user_id)
            values (:ownerId, :userId)
            on conflict (owner_id, user_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("ownerId") UUID ownerId, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ShareBlock b where b.ownerId = :ownerId and b.userId = :userId")
    int deleteBlock(@Param("ownerId") UUID ownerId, @Param("userId") UUID userId);
}
