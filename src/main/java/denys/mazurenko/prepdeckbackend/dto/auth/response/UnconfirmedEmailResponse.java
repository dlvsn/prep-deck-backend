package denys.mazurenko.prepdeckbackend.dto.auth.response;

/**
 * The data of an "auth.emailNotVerified" refusal at log in: the right password, an email not yet
 * confirmed. A code has been sent (unless one went out moments ago); the client opens the code
 * screen for {@code email}.
 */
public record UnconfirmedEmailResponse(String email,
                                       long resendAfterSeconds) {
}
