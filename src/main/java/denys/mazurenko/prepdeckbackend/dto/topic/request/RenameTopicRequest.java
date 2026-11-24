package denys.mazurenko.prepdeckbackend.dto.topic.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameTopicRequest(
        @NotBlank(message = "topics.titleRequired")
        @Size(max = 200, message = "topics.titleTooLong")
        String title
) {
}
