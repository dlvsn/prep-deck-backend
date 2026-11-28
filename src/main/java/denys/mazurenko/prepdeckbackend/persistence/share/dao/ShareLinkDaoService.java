package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareLinkDaoService {

    /** The link of the tree with this root. */
    Optional<ShareLink> findByRoot(UUID rootTopicId);

    Optional<ShareLink> findByToken(String token);

    List<ShareLink> findEnabledOfOwner(UUID ownerId);

    ShareLink save(ShareLink link);
}
