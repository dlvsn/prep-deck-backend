package denys.mazurenko.prepdeckbackend.service.topic;

import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.topic.dao.TopicDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The one place that decides whether a user may touch a topic and the cards in it. Today a topic
 * is reachable by its owner alone; sharing will widen this check, not the services that call it.
 * A topic of somebody else answers 404, so its id cannot be probed.
 */
@Component
@RequiredArgsConstructor
public class TopicGuard {

    private final TopicDaoService topics;

    public Topic require(UUID userId, UUID topicId) {
        return topics.findOwned(topicId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "errors.noAccess"));
    }
}
