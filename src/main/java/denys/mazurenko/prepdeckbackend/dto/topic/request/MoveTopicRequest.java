package denys.mazurenko.prepdeckbackend.dto.topic.request;

import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * A new place: under parentId (null: the top level), at {@code order} among the new siblings
 * counted without the topic itself. An order past the end puts it last.
 */
public record MoveTopicRequest(
        UUID parentId,

        @PositiveOrZero(message = "errors.badRequest")
        int order
) {
}
