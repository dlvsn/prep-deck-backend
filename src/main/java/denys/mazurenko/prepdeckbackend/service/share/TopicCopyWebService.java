package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.dto.share.request.CopyTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;

import java.util.UUID;

/** "Copy to my topics": a topic someone can see becomes theirs to change. */
public interface TopicCopyWebService {

    /**
     * Copies the topic with its subtopics, cards, tags and pictures (the pictures as files of the
     * caller's own) under {@code parentId} among the caller's topics, after its siblings, and
     * answers the copy. No practice progress comes along. Refuses "share.copyTooLarge" past the
     * configured number of cards.
     */
    TopicResponse copyToMine(UUID userId, UUID topicId, CopyTopicRequest request);
}
