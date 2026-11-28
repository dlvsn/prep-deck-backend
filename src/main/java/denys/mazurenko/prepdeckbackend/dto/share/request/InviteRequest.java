package denys.mazurenko.prepdeckbackend.dto.share.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Invites someone by email or username to the topics given, all of one tree. A topic brings its
 * whole subtree; a subtopic listed along with its parent adds nothing.
 */
public record InviteRequest(
        @NotBlank(message = "share.loginRequired")
        @Size(max = 320, message = "share.loginTooLong")
        String login,

        @NotEmpty(message = "share.pickOne")
        @Size(max = 500, message = "share.tooManyTopics")
        List<@NotNull(message = "share.pickOne") UUID> topicIds
) {
}
