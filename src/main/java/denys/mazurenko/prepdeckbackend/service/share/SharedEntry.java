package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;

import java.util.List;
import java.util.UUID;

/**
 * One topic of Shared with me before its card count is known: {@code parentId} is null where the
 * part the user sees starts, and {@code path} names the parents above it they cannot see.
 */
record SharedEntry(Topic topic, UUID parentId, UserResponse owner, TopicRole role, boolean unseen,
                   List<String> path) {
}
