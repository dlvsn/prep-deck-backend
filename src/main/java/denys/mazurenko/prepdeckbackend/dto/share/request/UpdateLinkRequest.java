package denys.mazurenko.prepdeckbackend.dto.share.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Turns the tree's public link on or off. {@code topicIds} null keeps the topics it opens (for a
 * link made just now: the topic the call was made on); given, it replaces them and may not be
 * empty.
 */
public record UpdateLinkRequest(
        @NotNull(message = "share.enabledRequired")
        Boolean enabled,

        @Size(max = 500, message = "share.tooManyTopics")
        List<@NotNull(message = "share.pickOne") UUID> topicIds
) {
}
