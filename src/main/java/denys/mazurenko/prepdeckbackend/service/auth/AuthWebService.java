package denys.mazurenko.prepdeckbackend.service.auth;

import denys.mazurenko.prepdeckbackend.dto.auth.request.LoginRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RefreshRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RegisterRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.response.SessionResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;

import java.util.UUID;

/**
 * Sign-up, log in, refresh, log out.
 *
 * <p>A session is a short-lived JWT plus a single-use refresh token. Every refresh revokes the
 * token it was given and issues the next one in the same family; a revoked token that comes back
 * can only be a stolen copy, so the whole family is revoked and both holders must log in again.
 */
public interface AuthWebService {

    /** Creates the account and opens its first session. Refuses a taken email or username (409). */
    SessionResponse register(RegisterRequest request);

    /** Opens a session by email or username. A wrong login and a wrong password look the same (401). */
    SessionResponse login(LoginRequest request);

    /** Spends the refresh token on a new access token and the next refresh token. */
    SessionResponse refresh(RefreshRequest request);

    /** Ends the session the refresh token belongs to; an unknown token is ignored. */
    void logout(RefreshRequest request);

    UserResponse me(UUID userId);

    boolean isUsernameFree(String username);
}
