package denys.mazurenko.prepdeckbackend.dto.card.request;

import jakarta.validation.constraints.Size;

/**
 * The copy's question, already in the user's language ("… (copy)", "… (копія)"); null keeps the
 * original's question as it is.
 */
public record DuplicateCardRequest(
        @Size(min = 1, max = 500, message = "editor.questionTooLong")
        String question
) {
}
