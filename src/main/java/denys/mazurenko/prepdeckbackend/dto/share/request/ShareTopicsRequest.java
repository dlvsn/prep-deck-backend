package denys.mazurenko.prepdeckbackend.dto.share.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** The whole new set of topics a person may see in one tree; it replaces the old set. */
public record ShareTopicsRequest(
        @NotEmpty(message = "share.pickOne")
        @Size(max = 500, message = "share.tooManyTopics")
        List<@NotNull(message = "share.pickOne") UUID> topicIds
) {
}
