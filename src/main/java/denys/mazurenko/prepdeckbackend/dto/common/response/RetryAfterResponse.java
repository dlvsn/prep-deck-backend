package denys.mazurenko.prepdeckbackend.dto.common.response;

/**
 * The data of a "too soon" refusal ("auth.resendTooSoon", "share.inviteTooSoon"): seconds until
 * the same letter may be asked for again.
 */
public record RetryAfterResponse(long retryAfterSeconds) {
}
