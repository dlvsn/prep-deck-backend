package denys.mazurenko.prepdeckbackend.service.auth;

import denys.mazurenko.prepdeckbackend.dto.auth.request.LoginRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RefreshRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RegisterRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.ResendCodeRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.VerifyEmailRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.response.RegistrationResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.SessionResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;

import java.util.Locale;
import java.util.UUID;

/**
 * Sign-up with email confirmation, log in, refresh, log out.
 *
 * <p>Sign-up creates the account unconfirmed and emails a six-digit code; the first session is
 * opened when that code comes back. A session is a short-lived JWT plus a single-use refresh
 * token. Every refresh revokes the token it was given and issues the next one in the same
 * family; a revoked token that comes back can only be a stolen copy, so the whole family is
 * revoked and both holders must log in again.
 *
 * <p>{@code locale} picks the language of the letter (Ukrainian or English).
 */
public interface AuthWebService {

    /**
     * Creates the account unconfirmed and sends the code. Refuses an email confirmed by another
     * account or a taken username (409); an email whose account was never confirmed is taken over.
     */
    RegistrationResponse register(RegisterRequest request, Locale locale);

    /** Confirms the email with the code and opens the first session. */
    SessionResponse verifyEmail(VerifyEmailRequest request);

    /** Sends a fresh code, unless the last went out less than the cooldown ago (429). */
    void resendVerification(ResendCodeRequest request, Locale locale);

    /**
     * Opens a session by email or username. A wrong login and a wrong password look the same
     * (401); the right password on an unconfirmed email sends a code and answers 403.
     */
    SessionResponse login(LoginRequest request, Locale locale);

    /** Spends the refresh token on a new access token and the next refresh token. */
    SessionResponse refresh(RefreshRequest request);

    /** Ends the session the refresh token belongs to; an unknown token is ignored. */
    void logout(RefreshRequest request);

    UserResponse me(UUID userId);

    boolean isUsernameFree(String username);
}
