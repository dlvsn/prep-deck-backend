package denys.mazurenko.prepdeckbackend.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Asks for a new confirmation letter. */
public record ResendCodeRequest(
        @NotBlank(message = "auth.emailRequired")
        @Email(message = "auth.emailInvalid")
        String email
) {
}
