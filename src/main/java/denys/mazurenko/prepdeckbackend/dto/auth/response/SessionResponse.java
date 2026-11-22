package denys.mazurenko.prepdeckbackend.dto.auth.response;

/**
 * What log in, sign up and refresh answer with (client type `Session`). The refresh token is
 * single-use: the next refresh must send the one returned here.
 */
public record SessionResponse(String accessToken, String refreshToken, UserResponse user) {
}
