package denys.mazurenko.prepdeckbackend.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** The code from the confirmation letter, for the email it was sent to. */
public record VerifyEmailRequest(
        @NotBlank(message = "auth.emailRequired")
        @Email(message = "auth.emailInvalid")
        String email,

        @NotBlank(message = "auth.codeIncomplete")
        @Pattern(regexp = "^\\d{6}$", message = "auth.codeIncomplete")
        String code
) {
}
