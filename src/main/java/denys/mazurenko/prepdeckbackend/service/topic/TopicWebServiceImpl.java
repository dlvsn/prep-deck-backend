package denys.mazurenko.prepdeckbackend.service.topic;

import denys.mazurenko.prepdeckbackend.domain.TopicAction;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.dto.topic.request.CreateTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.MoveTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.RenameTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.card.dao.CardDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareInviteDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareLinkDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.TopicGrantDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareInvite;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareLink;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.TopicGrant;
import denys.mazurenko.prepdeckbackend.persistence.topic.dao.TopicDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import denys.mazurenko.prepdeckbackend.persistence.topic.mapper.TopicMapper;
import denys.mazurenko.prepdeckbackend.persistence.user.dao.UserDaoService;
import denys.mazurenko.prepdeckbackend.persistence.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Every change goes through {@link TopicGuard} with {@link TopicAction#EDIT}, and works on the
 * owner's tree (the topic's owner, not necessarily the caller), so a role that may edit someone
 * else's topics later needs no change here.
 */
@Service
@RequiredArgsConstructor
public class TopicWebServiceImpl implements TopicWebService {

    private final TopicDaoService topics;
    private final CardDaoService cards;
    private final UserDaoService users;
    private final TopicGrantDaoService grants;
    private final ShareLinkDaoService links;
    private final ShareInviteDaoService invites;
    private final TopicGuard guard;
    private final TopicMapper topicMapper;
    private final UserMapper userMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> listMine(UUID userId) {
        UserResponse owner = user(userId);
        List<Topic> mine = topics.findByOwner(userId);
        Map<UUID, Long> counts = cards.countByTopic(mine.stream().map(Topic::getId).toList());
        Set<UUID> shared = sharedTopicIds(userId);
        return mine.stream()
                .map(topic -> topicMapper.toOwnResponse(topic, owner, counts.getOrDefault(topic.getId(), 0L),
                        shared.contains(topic.getId())))
                .toList();
    }

    @Override
    @Transactional
    public TopicResponse create(UUID userId, CreateTopicRequest request) {
        UUID ownerId = userId;
        if (request.parentId() != null) {
            ownerId = guard.require(userId, request.parentId(), TopicAction.EDIT).topic().getOwnerId();
        }
        Topic topic = new Topic();
        topic.setOwnerId(ownerId);
        topic.setParentId(request.parentId());
        topic.setTitle(request.title().trim());
        topic.setPosition(topics.findSiblings(ownerId, request.parentId()).size());
        return toResponse(topics.save(topic));
    }

    @Override
    @Transactional
    public TopicResponse rename(UUID userId, UUID topicId, RenameTopicRequest request) {
        Topic topic = guard.require(userId, topicId, TopicAction.EDIT).topic();
        topic.setTitle(request.title().trim());
        return toResponse(topics.save(topic));
    }

    /** One transaction: both renumberings land together or not at all. */
    @Override
    @Transactional
    public TopicResponse move(UUID userId, UUID topicId, MoveTopicRequest request) {
        Topic topic = guard.require(userId, topicId, TopicAction.EDIT).topic();
        UUID ownerId = topic.getOwnerId();
        UUID oldParentId = topic.getParentId();
        UUID newParentId = request.parentId();
        if (newParentId != null) {
            Topic newParent = guard.require(userId, newParentId, TopicAction.EDIT).topic();
            // A topic never changes owner, so it never leaves its owner's trees.
            if (!newParent.getOwnerId().equals(ownerId)) {
                throw TopicGuard.noAccess();
            }
            refuseCycle(ownerId, topicId, newParentId);
        }

        List<Topic> newSiblings = new ArrayList<>(topics.findSiblings(ownerId, newParentId));
        newSiblings.removeIf(sibling -> sibling.getId().equals(topicId));
        newSiblings.add(Math.min(request.order(), newSiblings.size()), topic);
        topic.setParentId(newParentId);
        renumber(newSiblings);

        if (!Objects.equals(oldParentId, newParentId)) {
            List<Topic> oldSiblings = new ArrayList<>(topics.findSiblings(ownerId, oldParentId));
            oldSiblings.removeIf(sibling -> sibling.getId().equals(topicId));
            renumber(oldSiblings);
        }
        return toResponse(topic);
    }

    /** Grants, links and invitations on the subtree go with it by cascade. */
    @Override
    @Transactional
    public void delete(UUID userId, UUID topicId) {
        Topic topic = guard.require(userId, topicId, TopicAction.EDIT).topic();
        topics.delete(topic);
        List<Topic> siblings = new ArrayList<>(topics.findSiblings(topic.getOwnerId(), topic.getParentId()));
        siblings.removeIf(sibling -> sibling.getId().equals(topicId));
        renumber(siblings);
    }

    /** Walks up from the new parent; meeting the topic on the way means it would go inside itself. */
    private void refuseCycle(UUID ownerId, UUID topicId, UUID newParentId) {
        if (new TopicForest(topics.findByOwner(ownerId)).isWithin(newParentId, topicId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "topics.moveIntoItself");
        }
    }

    private void renumber(List<Topic> siblings) {
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
        topics.saveAll(siblings);
    }

    /**
     * Topics someone else can see or has been invited to see: granted directly, opened by an
     * enabled public link, or listed in a pending invitation. Their subtopics are reachable too,
     * but only these carry the "shared" mark.
     */
    private Set<UUID> sharedTopicIds(UUID ownerId) {
        Set<UUID> shared = new HashSet<>();
        grants.findByOwner(ownerId).stream().map(TopicGrant::getTopicId).forEach(shared::add);
        links.findEnabledOfOwner(ownerId).stream().map(ShareLink::getTopicIds).forEach(shared::addAll);
        invites.findPendingOfOwner(ownerId, clock.instant()).stream().map(ShareInvite::getTopicIds).forEach(shared::addAll);
        return shared;
    }

    private TopicResponse toResponse(Topic topic) {
        UUID ownerId = topic.getOwnerId();
        long count = cards.countByTopic(List.of(topic.getId())).getOrDefault(topic.getId(), 0L);
        return topicMapper.toOwnResponse(topic, user(ownerId), count, sharedTopicIds(ownerId).contains(topic.getId()));
    }

    private UserResponse user(UUID userId) {
        return users.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(ApiException::sessionExpired);
    }
}
