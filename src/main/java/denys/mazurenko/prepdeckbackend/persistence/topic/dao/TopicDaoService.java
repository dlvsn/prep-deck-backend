package denys.mazurenko.prepdeckbackend.persistence.topic.dao;

import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicDaoService {

    List<Topic> findByOwner(UUID ownerId);

    Optional<Topic> findById(UUID id);

    /** The children of a topic, or the roots when parentId is null, in their order. */
    List<Topic> findSiblings(UUID ownerId, UUID parentId);

    Topic save(Topic topic);

    List<Topic> saveAll(List<Topic> topics);

    /** The database takes the subtree and its cards along. */
    void delete(Topic topic);
}
