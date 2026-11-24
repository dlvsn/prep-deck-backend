package denys.mazurenko.prepdeckbackend.persistence.topic.dao;

import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import denys.mazurenko.prepdeckbackend.persistence.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicDaoServiceImpl implements TopicDaoService {

    private final TopicRepository topicRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Topic> findByOwner(UUID ownerId) {
        return topicRepository.findByOwnerId(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Topic> findOwned(UUID id, UUID ownerId) {
        return topicRepository.findByIdAndOwnerId(id, ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> findSiblings(UUID ownerId, UUID parentId) {
        return parentId == null
                ? topicRepository.findByOwnerIdAndParentIdIsNullOrderByPositionAsc(ownerId)
                : topicRepository.findByOwnerIdAndParentIdOrderByPositionAsc(ownerId, parentId);
    }

    @Override
    @Transactional
    public Topic save(Topic topic) {
        return topicRepository.save(topic);
    }

    @Override
    @Transactional
    public List<Topic> saveAll(List<Topic> topics) {
        return topicRepository.saveAll(topics);
    }

    @Override
    @Transactional
    public void delete(Topic topic) {
        topicRepository.delete(topic);
    }
}
