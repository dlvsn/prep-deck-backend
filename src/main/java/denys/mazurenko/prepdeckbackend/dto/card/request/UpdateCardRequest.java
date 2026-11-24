package denys.mazurenko.prepdeckbackend.dto.card.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Changes to a card; a field left null stays as it is. {@code topicId} moves the card to another
 * topic. The card's practice progress is never touched by an edit.
 */
public record UpdateCardRequest(
        UUID topicId,

        @Size(min = 1, max = 500, message = "editor.questionTooLong")
        String question,

        @Size(max = 20000, message = "editor.answerTooLong")
        String answer,

        List<@NotNull(message = "errors.badRequest") @Size(max = 40, message = "editor.tagTooLong") String> tags,

        List<@NotNull(message = "errors.badRequest") UUID> imageIds
) {
}
