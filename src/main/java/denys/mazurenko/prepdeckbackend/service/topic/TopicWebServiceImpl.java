package denys.mazurenko.prepdeckbackend.service.topic;

import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.dto.topic.request.CreateTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.MoveTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.request.RenameTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.card.dao.CardDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.dao.TopicDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import denys.mazurenko.prepdeckbackend.persistence.topic.mapper.TopicMapper;
import denys.mazurenko.prepdeckbackend.persistence.user.dao.UserDaoService;
import denys.mazurenko.prepdeckbackend.persistence.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicWebServiceImpl implements TopicWebService {

    private final TopicDaoService topics;
    private final CardDaoService cards;
    private final UserDaoService users;
    private final TopicGuard guard;
    private final TopicMapper topicMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> listMine(UUID userId) {
        UserResponse owner = owner(userId);
        List<Topic> mine = topics.findByOwner(userId);
        Map<UUID, Long> counts = cards.countByTopic(mine.stream().map(Topic::getId).toList());
        return mine.stream()
                .map(topic -> topicMapper.toResponse(topic, owner, counts.getOrDefault(topic.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public TopicResponse create(UUID userId, CreateTopicRequest request) {
        if (request.parentId() != null) {
            guard.require(userId, request.parentId());
        }
        Topic topic = new Topic();
        topic.setOwnerId(userId);
        topic.setParentId(request.parentId());
        topic.setTitle(request.title().trim());
        topic.setPosition(topics.findSiblings(userId, request.parentId()).size());
        return topicMapper.toResponse(topics.save(topic), owner(userId), 0);
    }

    @Override
    @Transactional
    public TopicResponse rename(UUID userId, UUID topicId, RenameTopicRequest request) {
        Topic topic = guard.require(userId, topicId);
        topic.setTitle(request.title().trim());
        return toResponse(topics.save(topic), userId);
    }

    /** One transaction: both renumberings land together or not at all. */
    @Override
    @Transactional
    public TopicResponse move(UUID userId, UUID topicId, MoveTopicRequest request) {
        Topic topic = guard.require(userId, topicId);
        UUID oldParentId = topic.getParentId();
        UUID newParentId = request.parentId();
        if (newParentId != null) {
            guard.require(userId, newParentId);
            refuseCycle(userId, topicId, newParentId);
        }

        List<Topic> newSiblings = new ArrayList<>(topics.findSiblings(userId, newParentId));
        newSiblings.removeIf(sibling -> sibling.getId().equals(topicId));
        newSiblings.add(Math.min(request.order(), newSiblings.size()), topic);
        topic.setParentId(newParentId);
        renumber(newSiblings);

        if (!Objects.equals(oldParentId, newParentId)) {
            List<Topic> oldSiblings = new ArrayList<>(topics.findSiblings(userId, oldParentId));
            oldSiblings.removeIf(sibling -> sibling.getId().equals(topicId));
            renumber(oldSiblings);
        }
        return toResponse(topic, userId);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID topicId) {
        Topic topic = guard.require(userId, topicId);
        topics.delete(topic);
        List<Topic> siblings = new ArrayList<>(topics.findSiblings(userId, topic.getParentId()));
        siblings.removeIf(sibling -> sibling.getId().equals(topicId));
        renumber(siblings);
    }

    /** Walks up from the new parent; meeting the topic on the way means it would go inside itself. */
    private void refuseCycle(UUID userId, UUID topicId, UUID newParentId) {
        Map<UUID, Topic> byId = topics.findByOwner(userId).stream()
                .collect(Collectors.toMap(Topic::getId, Function.identity()));
        UUID cursor = newParentId;
        while (cursor != null) {
            if (cursor.equals(topicId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "topics.moveIntoItself");
            }
            Topic step = byId.get(cursor);
            cursor = step == null ? null : step.getParentId();
        }
    }

    private void renumber(List<Topic> siblings) {
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setPosition(i);
        }
        topics.saveAll(siblings);
    }

    private TopicResponse toResponse(Topic topic, UUID userId) {
        long count = cards.countByTopic(List.of(topic.getId())).getOrDefault(topic.getId(), 0L);
        return topicMapper.toResponse(topic, owner(userId), count);
    }

    private UserResponse owner(UUID userId) {
        return users.findById(userId).map(userMapper::toResponse).orElseThrow(ApiException::sessionExpired);
    }
}
