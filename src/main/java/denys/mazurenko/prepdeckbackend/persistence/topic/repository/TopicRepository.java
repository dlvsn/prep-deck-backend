package denys.mazurenko.prepdeckbackend.persistence.topic.repository;

import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findByOwnerId(UUID ownerId);

    List<Topic> findByOwnerIdAndParentIdOrderByPositionAsc(UUID ownerId, UUID parentId);

    List<Topic> findByOwnerIdAndParentIdIsNullOrderByPositionAsc(UUID ownerId);
}
