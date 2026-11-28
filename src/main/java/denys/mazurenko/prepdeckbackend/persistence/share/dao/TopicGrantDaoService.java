package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.GrantSource;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.TopicGrant;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicGrantDaoService {

    /** The strongest role the user holds on the topic through it or a topic above it. */
    Optional<TopicRole> findRoleOnTopic(UUID userId, UUID topicId);

    List<TopicGrant> findByGrantee(UUID granteeId);

    List<TopicGrant> findByOwner(UUID ownerId);

    /** Does nothing when the user already holds a grant on exactly this topic. */
    void grant(UUID topicId, UUID ownerId, UUID granteeId, TopicRole role, GrantSource source);

    void deleteAll(Collection<UUID> grantIds);

    void markSeen(UUID granteeId, Collection<UUID> topicIds, Instant now);
}
