package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.config.SharingProperties;
import denys.mazurenko.prepdeckbackend.domain.TopicAction;
import denys.mazurenko.prepdeckbackend.dto.share.request.CopyTopicRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.card.dao.CardDaoService;
import denys.mazurenko.prepdeckbackend.persistence.card.entity.Card;
import denys.mazurenko.prepdeckbackend.persistence.image.dao.ImageDaoService;
import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import denys.mazurenko.prepdeckbackend.persistence.image.storage.ImageFileStore;
import denys.mazurenko.prepdeckbackend.persistence.topic.dao.TopicDaoService;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import denys.mazurenko.prepdeckbackend.persistence.topic.mapper.TopicMapper;
import denys.mazurenko.prepdeckbackend.persistence.user.dao.UserDaoService;
import denys.mazurenko.prepdeckbackend.persistence.user.mapper.UserMapper;
import denys.mazurenko.prepdeckbackend.service.topic.TopicForest;
import denys.mazurenko.prepdeckbackend.service.topic.TopicGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Three steps, as an upload does: the subtree is read in one short transaction; the picture files
 * are copied inside the store with no transaction open, so a slow bucket never holds a database
 * connection; then every row of the copy is written in one transaction, all or nothing. If that
 * fails, the copied files are taken back out, so nothing is left in the store without a row.
 */
@Slf4j
@Service
public class TopicCopyWebServiceImpl implements TopicCopyWebService {

    private final TopicDaoService topics;
    private final CardDaoService cards;
    private final ImageDaoService images;
    private final UserDaoService users;
    private final ImageFileStore files;
    private final TopicGuard guard;
    private final TopicMapper topicMapper;
    private final UserMapper userMapper;
    private final SharingProperties properties;
    private final TransactionTemplate reading;
    private final TransactionTemplate writing;

    public TopicCopyWebServiceImpl(TopicDaoService topics, CardDaoService cards, ImageDaoService images,
                                   UserDaoService users, ImageFileStore files, TopicGuard guard, TopicMapper topicMapper,
                                   UserMapper userMapper, SharingProperties properties,
                                   PlatformTransactionManager transactionManager) {
        this.topics = topics;
        this.cards = cards;
        this.images = images;
        this.users = users;
        this.files = files;
        this.guard = guard;
        this.topicMapper = topicMapper;
        this.userMapper = userMapper;
        this.properties = properties;
        this.reading = new TransactionTemplate(transactionManager);
        this.reading.setReadOnly(true);
        this.writing = new TransactionTemplate(transactionManager);
    }

    @Override
    public TopicResponse copyToMine(UUID userId, UUID topicId, CopyTopicRequest request) {
        UUID parentId = request == null ? null : request.parentId();
        TopicCopyPlan plan = reading.execute(status -> plan(userId, topicId, parentId));

        Map<UUID, UUID> copiedFiles = new LinkedHashMap<>();
        try {
            for (Image image : plan.images()) {
                UUID copyId = UUID.randomUUID();
                files.copy(image.getId(), copyId);
                copiedFiles.put(image.getId(), copyId);
            }
            return writing.execute(status -> save(userId, plan, copiedFiles));
        } catch (RuntimeException e) {
            copiedFiles.values().forEach(this::removeQuietly);
            throw e;
        }
    }

    private TopicCopyPlan plan(UUID userId, UUID topicId, UUID parentId) {
        Topic source = guard.require(userId, topicId, TopicAction.VIEW).topic();
        if (parentId != null) {
            Topic parent = guard.require(userId, parentId, TopicAction.EDIT).topic();
            // The copy is the caller's own, so it goes into one of their own trees.
            if (!parent.getOwnerId().equals(userId)) {
                throw TopicGuard.noAccess();
            }
        }

        List<Topic> subtree = new TopicForest(topics.findByOwner(source.getOwnerId())).subtree(topicId);
        Map<UUID, List<CardCopy>> cardsByTopic = new HashMap<>();
        Map<UUID, Image> pictures = new LinkedHashMap<>();
        int cardTotal = 0;
        for (Topic topic : subtree) {
            // Newest first from the database; copied oldest first, so the newest stays on top.
            List<Card> found = new ArrayList<>(cards.findByTopic(topic.getId()));
            cardTotal += found.size();
            if (cardTotal > properties.getMaxCopyCards()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "share.copyTooLarge");
            }
            List<CardCopy> copies = new ArrayList<>();
            for (Card card : found.reversed()) {
                card.getImages().forEach(image -> pictures.putIfAbsent(image.getId(), image));
                copies.add(new CardCopy(card.getQuestion(), card.getAnswer(), List.copyOf(card.getTags()),
                        card.getImages().stream().map(Image::getId).toList()));
            }
            cardsByTopic.put(topic.getId(), copies);
        }
        return new TopicCopyPlan(parentId, subtree, cardsByTopic, List.copyOf(pictures.values()));
    }

    private TopicResponse save(UUID userId, TopicCopyPlan plan, Map<UUID, UUID> copiedFiles) {
        Map<UUID, Image> newImages = new HashMap<>();
        for (Image original : plan.images()) {
            Image copy = new Image();
            copy.setId(copiedFiles.get(original.getId()));
            copy.setOwnerId(userId);
            copy.setContentType(original.getContentType());
            copy.setSizeBytes(original.getSizeBytes());
            copy.setWidth(original.getWidth());
            copy.setHeight(original.getHeight());
            newImages.put(original.getId(), images.insert(copy));
        }

        Map<UUID, Topic> newTopics = new HashMap<>();
        Topic sourceRoot = plan.topics().getFirst();
        for (Topic original : plan.topics()) {
            boolean isRoot = original == sourceRoot;
            UUID newParentId = isRoot ? plan.parentId() : newTopics.get(original.getParentId()).getId();
            Topic copy = new Topic();
            copy.setOwnerId(userId);
            copy.setParentId(newParentId);
            copy.setTitle(original.getTitle());
            copy.setPosition(isRoot ? topics.findSiblings(userId, newParentId).size() : original.getPosition());
            newTopics.put(original.getId(), topics.save(copy));
        }

        for (Topic original : plan.topics()) {
            UUID newTopicId = newTopics.get(original.getId()).getId();
            for (CardCopy cardCopy : plan.cardsByTopic().get(original.getId())) {
                Card card = new Card();
                card.setTopicId(newTopicId);
                card.setQuestion(cardCopy.question());
                card.setAnswer(cardCopy.answer());
                card.setTags(new ArrayList<>(cardCopy.tags()));
                card.setImages(new ArrayList<>(cardCopy.imageIds().stream().map(newImages::get).toList()));
                cards.save(card);
            }
        }

        Topic root = newTopics.get(sourceRoot.getId());
        long rootCards = plan.cardsByTopic().get(sourceRoot.getId()).size();
        return topicMapper.toOwnResponse(root,
                users.findById(userId).map(userMapper::toResponse).orElseThrow(ApiException::sessionExpired),
                rootCards, false);
    }

    /** A failure is logged with the id, so the file can be swept by hand. */
    private void removeQuietly(UUID id) {
        try {
            files.delete(id);
        } catch (RuntimeException e) {
            log.error("Could not delete copied image {}; it has no row and must be removed by hand", id, e);
        }
    }
}
