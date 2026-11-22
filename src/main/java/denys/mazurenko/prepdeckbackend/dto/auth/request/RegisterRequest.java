package denys.mazurenko.prepdeckbackend.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sign-up. The messages are the client's i18n keys. The password rule matches the client's own
 * check; 72 is where BCrypt stops reading.
 */
public record RegisterRequest(
        @NotBlank(message = "auth.emailRequired")
        @Email(message = "auth.emailInvalid")
        @Size(max = 320, message = "auth.emailInvalid")
        String email,

        @NotBlank(message = "auth.usernameShort")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,32}$", message = "auth.usernameInvalid")
        String username,

        @NotBlank(message = "auth.passwordRequired")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).{8,72}$", message = "auth.passwordRules")
        String password
) {
}
