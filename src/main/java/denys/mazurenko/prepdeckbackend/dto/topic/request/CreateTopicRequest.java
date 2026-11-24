package denys.mazurenko.prepdeckbackend.dto.topic.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** A new topic at the end of its parent's children; parentId null makes a root. */
public record CreateTopicRequest(
        @NotBlank(message = "topics.titleRequired")
        @Size(max = 200, message = "topics.titleTooLong")
        String title,

        UUID parentId
) {
}
