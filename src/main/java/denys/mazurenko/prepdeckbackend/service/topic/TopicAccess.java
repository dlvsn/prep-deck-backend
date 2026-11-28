package denys.mazurenko.prepdeckbackend.service.topic;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;

/** A topic the caller was let through to, and the role that let them. */
public record TopicAccess(Topic topic, TopicRole role) {
}
