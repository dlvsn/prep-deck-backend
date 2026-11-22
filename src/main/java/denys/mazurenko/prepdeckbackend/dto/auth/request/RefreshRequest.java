package denys.mazurenko.prepdeckbackend.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/** Body of refresh and log out: the refresh token the client holds. */
public record RefreshRequest(@NotBlank(message = "errors.sessionExpired") String refreshToken) {
}
