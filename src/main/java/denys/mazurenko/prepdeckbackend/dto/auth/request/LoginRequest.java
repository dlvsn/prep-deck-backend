package denys.mazurenko.prepdeckbackend.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/** Log in with an email or a username. */
public record LoginRequest(
        @NotBlank(message = "auth.loginRequired") String login,
        @NotBlank(message = "auth.passwordRequired") String password
) {
}
