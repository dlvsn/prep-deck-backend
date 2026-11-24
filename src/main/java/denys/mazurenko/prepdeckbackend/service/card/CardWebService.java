package denys.mazurenko.prepdeckbackend.service.card;

import denys.mazurenko.prepdeckbackend.dto.card.request.CreateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.request.DuplicateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.request.UpdateCardRequest;
import denys.mazurenko.prepdeckbackend.dto.card.response.CardResponse;

import java.util.List;
import java.util.UUID;

/**
 * Cards in a user's topics. Every call goes through the topic the card sits in: a topic the user
 * cannot reach answers "errors.noAccess" (404) for its cards as well.
 */
public interface CardWebService {

    /** Cards directly in the topic, newest first. */
    List<CardResponse> list(UUID userId, UUID topicId);

    CardResponse get(UUID userId, UUID cardId);

    /**
     * Refuses "editor.questionRequired", "editor.tooManyTags", "editor.tooManyImages", and
     * "images.notFound" for an image id that is not an upload of the user's.
     */
    CardResponse create(UUID userId, UUID topicId, CreateCardRequest request);

    /** Applies the fields that are set; a topicId moves the card. Same refusals as create. */
    CardResponse update(UUID userId, UUID cardId, UpdateCardRequest request);

    void delete(UUID userId, UUID cardId);

    /** A copy in the same topic with the same tags and pictures, and no practice progress. */
    CardResponse duplicate(UUID userId, UUID cardId, DuplicateCardRequest request);

    /** Every tag on the user's cards, alphabetically, for suggestions while a tag is typed. */
    List<String> tags(UUID userId);
}
