package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.domain.TopicAction;
import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.dto.share.request.AcceptShareRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.card.dao.CardDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareBlockDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareInviteDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareLinkDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.TopicGrantDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.GrantSource;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareInvite;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareLink;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.TopicGrant;
import denys.mazurenko.prepdeckbackend.persistence.topic.dao.TopicDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import denys.mazurenko.prepdeckbackend.persistence.topic.mapper.TopicMapper;
import denys.mazurenko.prepdeckbackend.persistence.user.dao.UserDaoService;
import denys.mazurenko.prepdeckbackend.persistence.user.entity.User;
import denys.mazurenko.prepdeckbackend.persistence.user.mapper.UserMapper;
import denys.mazurenko.prepdeckbackend.security.OpaqueTokens;
import denys.mazurenko.prepdeckbackend.service.topic.TopicAccess;
import denys.mazurenko.prepdeckbackend.service.topic.TopicForest;
import denys.mazurenko.prepdeckbackend.service.topic.TopicGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SharedTopicWebServiceImpl implements SharedTopicWebService {

    private final TopicDaoService topics;
    private final CardDaoService cards;
    private final UserDaoService users;
    private final TopicGrantDaoService grants;
    private final ShareLinkDaoService links;
    private final ShareInviteDaoService invites;
    private final ShareBlockDaoService blocks;
    private final TopicGuard guard;
    private final TopicMapper topicMapper;
    private final UserMapper userMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> listShared(UUID userId) {
        Map<UUID, List<TopicGrant>> byOwner = grants.findByGrantee(userId).stream()
                .collect(Collectors.groupingBy(TopicGrant::getOwnerId, LinkedHashMap::new, Collectors.toList()));
        Map<UUID, User> owners = users.findAllById(byOwner.keySet()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<SharedEntry> entries = new ArrayList<>();
        byOwner.forEach((ownerId, held) -> {
            User owner = owners.get(ownerId);
            if (owner != null) {
                entries.addAll(entriesOf(new TopicForest(topics.findByOwner(ownerId)), userMapper.toResponse(owner), held));
            }
        });

        Map<UUID, Long> counts = cards.countByTopic(entries.stream().map(entry -> entry.topic().getId()).toList());
        return entries.stream()
                .sorted(Comparator.comparing((SharedEntry entry) -> entry.owner().name(), String.CASE_INSENSITIVE_ORDER))
                .map(entry -> topicMapper.toSharedResponse(entry.topic(), entry.parentId(), entry.owner(), entry.role(),
                        counts.getOrDefault(entry.topic().getId(), 0L), entry.unseen(), entry.path()))
                .toList();
    }

    @Override
    @Transactional
    public TopicResponse accept(UUID userId, AcceptShareRequest request) {
        String token = tokenOf(request.token());
        User user = users.findById(userId).orElseThrow(ApiException::sessionExpired);
        Optional<ShareLink> link = links.findByToken(token);
        UUID ownerId;
        Set<UUID> offered;
        GrantSource source;

        if (link.isPresent()) {
            ShareLink found = link.get();
            if (!found.isEnabled()) {
                throw linkInvalid();
            }
            ownerId = found.getOwnerId();
            refuseOwn(ownerId, userId);
            if (blocks.isBlocked(ownerId, userId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "share.accessRevoked");
            }
            offered = Set.copyOf(found.getTopicIds());
            source = GrantSource.LINK;
        } else {
            ShareInvite invite = invites.findByHashForUpdate(OpaqueTokens.hash(token))
                    .orElseThrow(SharedTopicWebServiceImpl::linkInvalid);
            Instant now = clock.instant();
            if (!invite.isPending(now)) {
                throw inviteExpired();
            }
            if (!invite.getEmail().equals(user.getEmail())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "share.inviteForAnotherEmail");
            }
            ownerId = invite.getOwnerId();
            refuseOwn(ownerId, userId);
            offered = Set.copyOf(invite.getTopicIds());
            source = GrantSource.INVITE;
            invite.setAcceptedAt(now);
            invites.save(invite);
            // A personal invitation is the owner's own choice, so it lifts an earlier removal.
            blocks.unblock(ownerId, userId);
        }

        TopicForest forest = new TopicForest(topics.findByOwner(ownerId));
        List<UUID> present = offered.stream().filter(forest::contains).toList();
        if (present.isEmpty()) {
            // Every topic it opened has been deleted since.
            throw source == GrantSource.LINK ? linkInvalid() : inviteExpired();
        }
        addGrants(forest, ownerId, userId, forest.outermost(present), source);

        UUID first = forest.inTreeOrder(present).getFirst();
        return listShared(userId).stream()
                .filter(topic -> topic.id().equals(first))
                .findFirst()
                .orElseThrow(TopicGuard::noAccess);
    }

    @Override
    @Transactional
    public void markSeen(UUID userId, UUID topicId) {
        TopicAccess access = guard.require(userId, topicId, TopicAction.VIEW);
        if (access.role() == TopicRole.OWNER) {
            return;
        }
        grants.markSeen(userId, chainOf(access.topic()), clock.instant());
    }

    @Override
    @Transactional
    public void leave(UUID userId, UUID topicId) {
        TopicAccess access = guard.require(userId, topicId, TopicAction.VIEW);
        refuseOwn(access.topic().getOwnerId(), userId);
        Set<UUID> chain = Set.copyOf(chainOf(access.topic()));
        grants.deleteAll(grants.findByGrantee(userId).stream()
                .filter(grant -> chain.contains(grant.getTopicId()))
                .map(TopicGrant::getId)
                .toList());
    }

    /**
     * Adds what was offered to what the user already holds of this owner's, keeping only the
     * outermost grants: a grant under a topic now granted whole is dropped, one under a topic
     * already held is never made.
     */
    private void addGrants(TopicForest forest, UUID ownerId, UUID userId, Set<UUID> offered, GrantSource source) {
        List<TopicGrant> held = grants.findByGrantee(userId).stream()
                .filter(grant -> grant.getOwnerId().equals(ownerId) && forest.contains(grant.getTopicId()))
                .toList();
        Set<UUID> heldIds = held.stream().map(TopicGrant::getTopicId).collect(Collectors.toSet());
        Set<UUID> wanted = forest.outermost(Stream.concat(heldIds.stream(), offered.stream()).toList());

        grants.deleteAll(held.stream()
                .filter(grant -> !wanted.contains(grant.getTopicId()))
                .map(TopicGrant::getId)
                .toList());
        wanted.stream()
                .filter(id -> !heldIds.contains(id))
                .forEach(id -> grants.grant(id, ownerId, userId, TopicRole.VIEWER, source));
    }

    /** What the user sees of one owner's trees, parents before children. */
    private static List<SharedEntry> entriesOf(TopicForest forest, UserResponse owner, List<TopicGrant> held) {
        Map<UUID, TopicGrant> byTopic = held.stream()
                .filter(grant -> forest.contains(grant.getTopicId()))
                .collect(Collectors.toMap(TopicGrant::getTopicId, Function.identity(), (a, b) -> a));
        List<SharedEntry> entries = new ArrayList<>();
        for (UUID rootId : forest.inTreeOrder(forest.outermost(byTopic.keySet()))) {
            TopicGrant grant = byTopic.get(rootId);
            List<String> path = forest.ancestors(rootId).stream().map(Topic::getTitle).toList();
            for (Topic topic : forest.subtree(rootId)) {
                boolean top = topic.getId().equals(rootId);
                entries.add(new SharedEntry(topic, top ? null : topic.getParentId(), owner, grant.getRole(),
                        top && grant.getSeenAt() == null, top ? path : List.of()));
            }
        }
        return entries;
    }

    /** The topic and every topic above it: the grants it may have come through. */
    private List<UUID> chainOf(Topic topic) {
        TopicForest forest = new TopicForest(topics.findByOwner(topic.getOwnerId()));
        List<UUID> chain = new ArrayList<>(forest.ancestors(topic.getId()).stream().map(Topic::getId).toList());
        chain.add(topic.getId());
        return chain;
    }

    /** A pasted link is fine too: the token is what follows its last "/". */
    private static String tokenOf(String raw) {
        String value = raw.strip();
        return value.substring(value.lastIndexOf('/') + 1);
    }

    private static void refuseOwn(UUID ownerId, UUID userId) {
        if (ownerId.equals(userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "share.ownTopic");
        }
    }

    private static ApiException linkInvalid() {
        return new ApiException(HttpStatus.NOT_FOUND, "share.linkInvalid");
    }

    private static ApiException inviteExpired() {
        return new ApiException(HttpStatus.GONE, "share.inviteExpired");
    }
}
