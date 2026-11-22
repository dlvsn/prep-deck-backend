package denys.mazurenko.prepdeckbackend.dto.auth.response;

/**
 * Sign-up accepted: the account waits for its email to be confirmed, and a letter with the code
 * is on its way to {@code email}. The client shows the code screen with a resend timer.
 */
public record RegistrationResponse(String email,
                                   long codeTtlMinutes,
                                   long resendAfterSeconds) {
}
