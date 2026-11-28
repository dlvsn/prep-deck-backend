package denys.mazurenko.prepdeckbackend.persistence.share.repository;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {

    /** The link of the tree with this root. */
    Optional<ShareLink> findByTopicId(UUID rootTopicId);

    Optional<ShareLink> findByToken(String token);

    List<ShareLink> findByOwnerIdAndEnabledTrue(UUID ownerId);
}
