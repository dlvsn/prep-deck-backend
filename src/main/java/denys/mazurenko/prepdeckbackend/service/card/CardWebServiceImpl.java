package denys.mazurenko.prepdeckbackend.service.card;

import denys.mazurenko.prepdeckbackend.config.CardProperties;
import denys.mazurenko.prepdeckbackend.config.ImageProperties;
import denys.mazurenko.prepdeckbackend.dto.card.request.CreateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.request.DuplicateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.request.UpdateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.response.CardResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.card.dao.CardDaoService;
import denys.mazurenko.prepdeckbackend.persistence.card.entity.Card;
import denys.mazurenko.prepdeckbackend.persistence.card.mapper.CardMapper;
import denys.mazurenko.prepdeckbackend.persistence.image.dao.ImageDaoService;
import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import denys.mazurenko.prepdeckbackend.service.topic.TopicGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads run in a transaction too: a card's tags and pictures load lazily while it is mapped.
 */
@Service
@RequiredArgsConstructor
public class CardWebServiceImpl implements CardWebService {

    private final CardDaoService cards;
    private final ImageDaoService images;
    private final TopicGuard guard;
    private final CardMapper cardMapper;
    private final CardProperties cardProperties;
    private final ImageProperties imageProperties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> list(UUID userId, UUID topicId) {
        guard.require(userId, topicId);
        return cards.findByTopic(topicId).stream().map(cardMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse get(UUID userId, UUID cardId) {
        return cardMapper.toResponse(reachable(userId, cardId));
    }

    @Override
    @Transactional
    public CardResponse create(UUID userId, UUID topicId, CreateCardRequest request) {
        guard.require(userId, topicId);
        Card card = new Card();
        card.setTopicId(topicId);
        card.setQuestion(question(request.question()));
        card.setAnswer(request.answer());
        card.setTags(tags(request.tags()));
        card.setImages(images(userId, request.imageIds(), List.of()));
        return cardMapper.toResponse(cards.save(card));
    }

    @Override
    @Transactional
    public CardResponse update(UUID userId, UUID cardId, UpdateCardRequest request) {
        Card card = reachable(userId, cardId);
        if (request.topicId() != null && !request.topicId().equals(card.getTopicId())) {
            guard.require(userId, request.topicId());
            card.setTopicId(request.topicId());
        }
        if (request.question() != null) {
            card.setQuestion(question(request.question()));
        }
        if (request.answer() != null) {
            card.setAnswer(request.answer());
        }
        if (request.tags() != null) {
            card.setTags(tags(request.tags()));
        }
        if (request.imageIds() != null) {
            card.setImages(images(userId, request.imageIds(), card.getImages()));
        }
        // Tags and pictures live in their own tables; this makes the row itself count as changed.
        card.setUpdatedAt(clock.instant());
        return cardMapper.toResponse(cards.save(card));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID cardId) {
        cards.delete(reachable(userId, cardId));
    }

    @Override
    @Transactional
    public CardResponse duplicate(UUID userId, UUID cardId, DuplicateCardRequest request) {
        Card original = reachable(userId, cardId);
        Card copy = new Card();
        copy.setTopicId(original.getTopicId());
        copy.setQuestion(request != null && request.question() != null
                ? question(request.question())
                : original.getQuestion());
        copy.setAnswer(original.getAnswer());
        copy.setTags(new ArrayList<>(original.getTags()));
        copy.setImages(new ArrayList<>(original.getImages()));
        return cardMapper.toResponse(cards.save(copy));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> tags(UUID userId) {
        return cards.findTagsOfOwner(userId);
    }

    /** A card the user may work with; anything else looks as if it did not exist. */
    private Card reachable(UUID userId, UUID cardId) {
        Card card = cards.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "errors.noAccess"));
        guard.require(userId, card.getTopicId());
        return card;
    }

    private static String question(String raw) {
        String question = raw.trim();
        if (question.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "editor.questionRequired");
        }
        return question;
    }

    /** Lower case, no leading "#", blanks and repeats dropped, first spelling's order kept. */
    private List<String> tags(List<String> raw) {
        Set<String> clean = new LinkedHashSet<>();
        for (String tag : raw) {
            String value = tag.trim().replaceFirst("^#+", "").trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                clean.add(value);
            }
        }
        if (clean.size() > cardProperties.getMaxTags()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "editor.tooManyTags");
        }
        return new ArrayList<>(clean);
    }

    /**
     * The pictures in the order given. An image may go on a card when the user uploaded it, or
     * when the card already shows it (a duplicate keeps its original's pictures).
     */
    private List<Image> images(UUID userId, List<UUID> ids, List<Image> current) {
        List<UUID> wanted = ids.stream().distinct().toList();
        if (wanted.size() > imageProperties.getMaxPerCard()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "editor.tooManyImages");
        }
        Set<UUID> alreadyOnCard = current.stream().map(Image::getId).collect(Collectors.toSet());
        Map<UUID, Image> found = images.findAllById(wanted).stream()
                .filter(image -> image.getOwnerId().equals(userId) || alreadyOnCard.contains(image.getId()))
                .collect(Collectors.toMap(Image::getId, Function.identity()));
        if (found.size() != wanted.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "images.notFound");
        }
        return new ArrayList<>(wanted.stream().map(found::get).toList());
    }
}
