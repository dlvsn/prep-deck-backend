package denys.mazurenko.prepdeckbackend.dto.share.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A public link or an invitation link, as pasted or opened: the token alone or the whole link
 * (everything after its last "/" is taken).
 */
public record AcceptShareRequest(
        @NotBlank(message = "share.linkInvalid")
        @Size(max = 2000, message = "share.linkInvalid")
        String token
) {
}
