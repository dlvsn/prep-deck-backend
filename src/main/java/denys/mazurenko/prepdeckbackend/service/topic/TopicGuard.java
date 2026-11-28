package denys.mazurenko.prepdeckbackend.service.topic;

import denys.mazurenko.prepdeckbackend.domain.TopicAction;
import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.share.dao.TopicGrantDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.dao.TopicDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * The one place that decides whether a user may touch a topic and the cards in it. The owner holds
 * {@link TopicRole#OWNER}; anyone else holds whatever grant reaches the topic, on it or on a topic
 * above it, and {@link TopicRole#allows} says what that role may do.
 *
 * <p>Grants are read on every call, never cached and never carried in a token, so taking one away
 * locks its holder out at their next request, whatever link they still have. A topic the user
 * cannot see answers 404 "errors.noAccess", so its id cannot be probed; one they can see but not
 * change answers 403 "errors.readOnly".
 */
@Component
@RequiredArgsConstructor
public class TopicGuard {

    private final TopicDaoService topics;
    private final TopicGrantDaoService grants;

    public TopicAccess require(UUID userId, UUID topicId, TopicAction action) {
        Topic topic = topics.findById(topicId).orElseThrow(TopicGuard::noAccess);
        TopicRole role = roleOf(userId, topic).orElseThrow(TopicGuard::noAccess);
        if (!role.allows(action)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "errors.readOnly");
        }
        return new TopicAccess(topic, role);
    }

    /** Empty when the user may not even see the topic. */
    public Optional<TopicRole> roleOf(UUID userId, Topic topic) {
        if (topic.getOwnerId().equals(userId)) {
            return Optional.of(TopicRole.OWNER);
        }
        return grants.findRoleOnTopic(userId, topic.getId());
    }

    public static ApiException noAccess() {
        return new ApiException(HttpStatus.NOT_FOUND, "errors.noAccess");
    }
}
