package denys.mazurenko.prepdeckbackend.dto.card.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * A new card. Tags are normalised by the server (lower case, no "#", no duplicates); imageIds are
 * uploads of the caller's, in display order.
 */
public record CreateCardRequest(
        @NotBlank(message = "editor.questionRequired")
        @Size(max = 500, message = "editor.questionTooLong")
        String question,

        @NotNull(message = "errors.badRequest")
        @Size(max = 20000, message = "editor.answerTooLong")
        String answer,

        @NotNull(message = "errors.badRequest")
        List<@NotNull(message = "errors.badRequest") @Size(max = 40, message = "editor.tagTooLong") String> tags,

        @NotNull(message = "errors.badRequest")
        List<@NotNull(message = "errors.badRequest") UUID> imageIds
) {
}
