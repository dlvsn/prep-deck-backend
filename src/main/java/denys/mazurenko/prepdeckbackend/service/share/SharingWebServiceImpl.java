package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.config.SharingProperties;
import denys.mazurenko.prepdeckbackend.domain.Language;
import denys.mazurenko.prepdeckbackend.domain.TopicAction;
import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.dto.common.response.RetryAfterResponse;
import denys.mazurenko.prepdeckbackend.dto.share.request.InviteRequest;
import denys.mazurenko.prepdeckbackend.dto.share.request.ShareTopicsRequest;
import denys.mazurenko.prepdeckbackend.dto.share.request.UpdateLinkRequest;
import denys.mazurenko.prepdeckbackend.dto.share.response.ShareInviteResponse;
import denys.mazurenko.prepdeckbackend.dto.share.response.ShareLinkResponse;
import denys.mazurenko.prepdeckbackend.dto.share.response.ShareMemberResponse;
import denys.mazurenko.prepdeckbackend.dto.share.response.SharingResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.mail.InvitedTopic;
import denys.mazurenko.prepdeckbackend.mail.ShareInviteIssued;
import denys.mazurenko.prepdeckbackend.persistence.card.dao.CardDaoService;
import denys.mazurenko.prepdeckbackend.persistence.settings.dao.UserSettingsDaoService;
import denys.mazurenko.prepdeckbackend.persistence.settings.entity.UserSettings;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareBlockDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareInviteDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.ShareLinkDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.TopicGrantDaoService;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.GrantSource;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareInvite;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareLink;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.TopicGrant;
import denys.mazurenko.prepdeckbackend.persistence.share.mapper.ShareMapper;
import denys.mazurenko.prepdeckbackend.persistence.topic.dao.TopicDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import denys.mazurenko.prepdeckbackend.persistence.user.dao.UserDaoService;
import denys.mazurenko.prepdeckbackend.persistence.user.entity.User;
import denys.mazurenko.prepdeckbackend.persistence.user.mapper.UserMapper;
import denys.mazurenko.prepdeckbackend.security.OpaqueTokens;
import denys.mazurenko.prepdeckbackend.service.topic.TopicForest;
import denys.mazurenko.prepdeckbackend.service.topic.TopicGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Each method is one transaction: replacing a person's grants, or revoking an old invitation and
 * saving its successor, must land together. Letters go out only after it commits
 * ({@link ShareInviteIssued}).
 */
@Service
@RequiredArgsConstructor
public class SharingWebServiceImpl implements SharingWebService {

    /** Enough to catch a typo; whether the address exists only the letter can tell. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final TopicDaoService topics;
    private final CardDaoService cards;
    private final UserDaoService users;
    private final UserSettingsDaoService settings;
    private final TopicGrantDaoService grants;
    private final ShareLinkDaoService links;
    private final ShareInviteDaoService invites;
    private final ShareBlockDaoService blocks;
    private final TopicGuard guard;
    private final UserMapper userMapper;
    private final ShareMapper shareMapper;
    private final SharingProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SharingResponse get(UUID userId, UUID topicId) {
        return view(tree(userId, topicId));
    }

    @Override
    @Transactional
    public SharingResponse invite(UUID userId, UUID topicId, InviteRequest request, Locale locale) {
        SharedTree tree = tree(userId, topicId);
        Set<UUID> scope = scope(tree, request.topicIds());
        User owner = users.findById(tree.ownerId()).orElseThrow(ApiException::sessionExpired);

        String login = request.login().strip().toLowerCase(Locale.ROOT);
        Optional<User> invitee;
        String email;
        if (login.contains("@")) {
            if (!EMAIL.matcher(login).matches()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "share.invalidEmail");
            }
            email = login;
            invitee = users.findByLogin(login);
        } else {
            // Only a confirmed account is somebody's: an unconfirmed one may still be taken over.
            User found = users.findByLogin(login)
                    .filter(user -> user.getEmailVerifiedAt() != null)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "share.noSuchUser"));
            email = found.getEmail();
            invitee = Optional.of(found);
        }
        if (email.equals(owner.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "share.selfInvite");
        }
        if (invitee.isPresent() && !grantsOf(tree, invitee.get().getId()).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "share.alreadyHasAccess");
        }

        Instant now = clock.instant();
        Duration wait = cooldownLeft(owner.getId(), email, now);
        if (!wait.isZero()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "share.inviteTooSoon",
                    new RetryAfterResponse(wait.toSeconds()));
        }

        // One waiting letter per address and tree: the new one replaces the last.
        List<ShareInvite> replaced = pendingIn(tree, now).stream()
                .filter(invite -> invite.getEmail().equals(email))
                .toList();
        replaced.forEach(invite -> invite.setRevokedAt(now));
        invites.saveAll(replaced);

        String token = OpaqueTokens.generate();
        ShareInvite invite = new ShareInvite();
        invite.setTopicId(tree.root().getId());
        invite.setOwnerId(owner.getId());
        invite.setEmail(email);
        invite.setTokenHash(OpaqueTokens.hash(token));
        invite.setExpiresAt(now.plus(properties.getInviteTtl()));
        invite.getTopicIds().addAll(scope);
        invites.save(invite);

        events.publishEvent(letter(tree, owner, email, invitee, token, scope, locale));
        return view(tree);
    }

    @Override
    @Transactional
    public SharingResponse revokeInvite(UUID userId, UUID topicId, UUID inviteId) {
        SharedTree tree = tree(userId, topicId);
        Instant now = clock.instant();
        ShareInvite invite = pendingIn(tree, now).stream()
                .filter(candidate -> candidate.getId().equals(inviteId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "share.inviteNotFound"));
        invite.setRevokedAt(now);
        invites.save(invite);
        return view(tree);
    }

    @Override
    @Transactional
    public SharingResponse changeTopics(UUID userId, UUID topicId, UUID memberId, ShareTopicsRequest request) {
        SharedTree tree = tree(userId, topicId);
        List<TopicGrant> held = grantsOf(tree, memberId);
        if (held.isEmpty()) {
            throw notMember();
        }
        Set<UUID> scope = scope(tree, request.topicIds());
        TopicRole role = strongest(held);

        grants.deleteAll(held.stream()
                .filter(grant -> !scope.contains(grant.getTopicId()))
                .map(TopicGrant::getId)
                .toList());
        Set<UUID> kept = held.stream().map(TopicGrant::getTopicId).collect(Collectors.toSet());
        // The owner picked these topics for the person, whatever brought them first.
        scope.stream()
                .filter(id -> !kept.contains(id))
                .forEach(id -> grants.grant(id, tree.ownerId(), memberId, role, GrantSource.INVITE));
        return view(tree);
    }

    @Override
    @Transactional
    public SharingResponse removeMember(UUID userId, UUID topicId, UUID memberId) {
        SharedTree tree = tree(userId, topicId);
        List<TopicGrant> held = grantsOf(tree, memberId);
        if (held.isEmpty()) {
            throw notMember();
        }
        takeAccess(tree, memberId, held);
        return view(tree);
    }

    @Override
    @Transactional
    public SharingResponse updateLink(UUID userId, UUID topicId, UpdateLinkRequest request) {
        SharedTree tree = tree(userId, topicId);
        ShareLink link = links.findByRoot(tree.root().getId()).orElseGet(() -> newLink(tree));
        if (request.topicIds() != null) {
            Set<UUID> scope = scope(tree, request.topicIds());
            link.getTopicIds().clear();
            link.getTopicIds().addAll(scope);
        } else if (link.getTopicIds().isEmpty()) {
            link.getTopicIds().add(topicId);
        }
        link.setEnabled(request.enabled());
        links.save(link);
        return view(tree);
    }

    @Override
    @Transactional
    public SharingResponse resetLink(UUID userId, UUID topicId) {
        SharedTree tree = tree(userId, topicId);
        ShareLink link = links.findByRoot(tree.root().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "share.noLink"));
        link.setToken(OpaqueTokens.generate());
        links.save(link);
        return view(tree);
    }

    @Override
    @Transactional
    public SharingResponse removeLinkMembers(UUID userId, UUID topicId) {
        SharedTree tree = tree(userId, topicId);
        membersOf(tree).forEach((memberId, held) -> {
            if (cameByLink(held)) {
                takeAccess(tree, memberId, held);
            }
        });
        return view(tree);
    }

    // ---------------------------------------------------------------- the tree and its people

    private SharedTree tree(UUID userId, UUID topicId) {
        Topic topic = guard.require(userId, topicId, TopicAction.SHARE).topic();
        TopicForest forest = new TopicForest(topics.findByOwner(topic.getOwnerId()));
        return new SharedTree(topic.getOwnerId(), forest.root(topic.getId()), forest);
    }

    /** Valid, inside the tree, and without a topic under another one picked. */
    private static Set<UUID> scope(SharedTree tree, List<UUID> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "share.pickOne");
        }
        for (UUID id : topicIds) {
            if (!tree.contains(id)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "share.topicNotInTree");
            }
        }
        return tree.forest().outermost(topicIds);
    }

    /** Everyone's grants inside the tree, by person, in the order they first got one. */
    private Map<UUID, List<TopicGrant>> membersOf(SharedTree tree) {
        return grants.findByOwner(tree.ownerId()).stream()
                .filter(grant -> tree.contains(grant.getTopicId()))
                .sorted(Comparator.comparing(TopicGrant::getCreatedAt))
                .collect(Collectors.groupingBy(TopicGrant::getGranteeId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<TopicGrant> grantsOf(SharedTree tree, UUID memberId) {
        return membersOf(tree).getOrDefault(memberId, List.of());
    }

    private List<ShareInvite> pendingIn(SharedTree tree, Instant now) {
        return invites.findPendingOfOwner(tree.ownerId(), now).stream()
                .filter(invite -> tree.contains(invite.getTopicId()))
                .toList();
    }

    /**
     * Their grants in this tree go, a letter still waiting for them here stops working, and the
     * owner's public links stop letting them in.
     */
    private void takeAccess(SharedTree tree, UUID memberId, List<TopicGrant> held) {
        grants.deleteAll(held.stream().map(TopicGrant::getId).toList());
        users.findById(memberId).ifPresent(member -> {
            Instant now = clock.instant();
            List<ShareInvite> waiting = pendingIn(tree, now).stream()
                    .filter(invite -> invite.getEmail().equals(member.getEmail()))
                    .toList();
            waiting.forEach(invite -> invite.setRevokedAt(now));
            invites.saveAll(waiting);
        });
        blocks.block(tree.ownerId(), memberId);
    }

    private static boolean cameByLink(List<TopicGrant> held) {
        return held.stream().allMatch(grant -> grant.getSource() == GrantSource.LINK);
    }

    private static TopicRole strongest(Collection<TopicGrant> held) {
        return held.stream().map(TopicGrant::getRole).min(Comparator.naturalOrder()).orElse(TopicRole.VIEWER);
    }

    private ShareLink newLink(SharedTree tree) {
        ShareLink link = new ShareLink();
        link.setTopicId(tree.root().getId());
        link.setOwnerId(tree.ownerId());
        link.setToken(OpaqueTokens.generate());
        return link;
    }

    private Duration cooldownLeft(UUID ownerId, String email, Instant now) {
        return invites.findLatest(ownerId, email)
                .map(last -> Duration.between(now, last.getCreatedAt().plus(properties.getInviteCooldown())))
                .filter(Duration::isPositive)
                .orElse(Duration.ZERO);
    }

    private static ApiException notMember() {
        return new ApiException(HttpStatus.NOT_FOUND, "share.notMember");
    }

    // ---------------------------------------------------------------- answers

    private SharingResponse view(SharedTree tree) {
        Instant now = clock.instant();
        Map<UUID, List<TopicGrant>> members = membersOf(tree);
        Map<UUID, User> people = users.findAllById(members.keySet()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ShareMemberResponse> memberResponses = members.entrySet().stream()
                .filter(entry -> people.containsKey(entry.getKey()))
                .map(entry -> new ShareMemberResponse(
                        userMapper.toResponse(people.get(entry.getKey())),
                        strongest(entry.getValue()),
                        cameByLink(entry.getValue()),
                        tree.inTreeOrder(entry.getValue().stream().map(TopicGrant::getTopicId).toList()),
                        entry.getValue().getFirst().getCreatedAt()))
                .toList();

        List<ShareInviteResponse> inviteResponses = pendingIn(tree, now).stream()
                .map(invite -> shareMapper.toResponse(invite, tree.inTreeOrder(invite.getTopicIds())))
                .toList();

        int joined = (int) members.values().stream().filter(SharingWebServiceImpl::cameByLink).count();
        ShareLinkResponse link = links.findByRoot(tree.root().getId())
                .map(found -> new ShareLinkResponse(
                        found.isEnabled(),
                        found.isEnabled() ? found.getToken() : null,
                        found.isEnabled() ? properties.getLinkBaseUrl() + found.getToken() : null,
                        tree.inTreeOrder(found.getTopicIds()),
                        joined))
                .orElse(new ShareLinkResponse(false, null, null, List.of(), joined));

        User owner = users.findById(tree.ownerId()).orElseThrow(ApiException::sessionExpired);
        return new SharingResponse(tree.root().getId(), userMapper.toResponse(owner), memberResponses,
                inviteResponses, link);
    }

    /** Everything the letter shows, read now while the transaction is open. */
    private ShareInviteIssued letter(SharedTree tree, User owner, String email, Optional<User> invitee, String token,
                                     Set<UUID> scope, Locale locale) {
        TopicForest forest = tree.forest();
        List<UUID> covered = scope.stream()
                .flatMap(id -> forest.subtree(id).stream())
                .map(Topic::getId)
                .toList();
        Map<UUID, Long> counts = cards.countByTopic(covered);

        List<InvitedTopic> rows = tree.inTreeOrder(scope).stream()
                .map(id -> {
                    Topic topic = forest.find(id).orElseThrow();
                    long cardCount = forest.subtree(id).stream()
                            .mapToLong(below -> counts.getOrDefault(below.getId(), 0L))
                            .sum();
                    List<String> path = forest.ancestors(id).stream().map(Topic::getTitle).toList();
                    return new InvitedTopic(topic.getTitle(), path, cardCount, forest.hasChildren(id));
                })
                .toList();
        long total = rows.stream().mapToLong(InvitedTopic::cardCount).sum();

        Language language = invitee
                .flatMap(user -> settings.find(user.getId()))
                .map(UserSettings::getLanguage)
                .orElse(Language.fromLocale(locale));
        return new ShareInviteIssued(email, language, owner.getDisplayName(), owner.getEmail(),
                properties.getLinkBaseUrl() + token, rows, total);
    }
}
