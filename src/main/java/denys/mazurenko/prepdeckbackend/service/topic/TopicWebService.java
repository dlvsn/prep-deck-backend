package denys.mazurenko.prepdeckbackend.service.topic;

import denys.mazurenko.prepdeckbackend.dto.topic.request.CreateTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.MoveTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.RenameTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;

import java.util.List;
import java.util.UUID;

/** A user's topic tree: read flat, grown, renamed, rearranged and pruned. */
public interface TopicWebService {

    /** Every topic the user owns, flat; parentId and order build the tree. */
    List<TopicResponse> listMine(UUID userId);

    /** Adds the topic after its new siblings. Refuses a parent the user cannot reach. */
    TopicResponse create(UUID userId, CreateTopicRequest request);

    TopicResponse rename(UUID userId, UUID topicId, RenameTopicRequest request);

    /**
     * Puts the topic under a new parent at the given place, renumbering both sets of siblings.
     * Refuses "topics.moveIntoItself" when the parent is the topic or lies in its subtree.
     */
    TopicResponse move(UUID userId, UUID topicId, MoveTopicRequest request);

    /** Deletes the topic with its subtopics and every card in them. */
    void delete(UUID userId, UUID topicId);
}
