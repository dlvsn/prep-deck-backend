package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.GrantSource;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.TopicGrant;
import denys.mazurenko.prepdeckbackend.persistence.share.repository.TopicGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicGrantDaoServiceImpl implements TopicGrantDaoService {

    private final TopicGrantRepository grantRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TopicRole> findRoleOnTopic(UUID userId, UUID topicId) {
        return grantRepository.findRolesOnTopic(userId, topicId).stream()
                .map(TopicRole::valueOf)
                .min(Comparator.naturalOrder());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicGrant> findByGrantee(UUID granteeId) {
        return grantRepository.findByGranteeId(granteeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicGrant> findByOwner(UUID ownerId) {
        return grantRepository.findByOwnerId(ownerId);
    }

    @Override
    @Transactional
    public void grant(UUID topicId, UUID ownerId, UUID granteeId, TopicRole role, GrantSource source) {
        grantRepository.insertIfAbsent(topicId, ownerId, granteeId, role.name(), source.name());
    }

    @Override
    @Transactional
    public void deleteAll(Collection<UUID> grantIds) {
        if (!grantIds.isEmpty()) {
            grantRepository.deleteByIds(grantIds);
        }
    }

    @Override
    @Transactional
    public void markSeen(UUID granteeId, Collection<UUID> topicIds, Instant now) {
        if (!topicIds.isEmpty()) {
            grantRepository.markSeen(granteeId, topicIds, now);
        }
    }
}
