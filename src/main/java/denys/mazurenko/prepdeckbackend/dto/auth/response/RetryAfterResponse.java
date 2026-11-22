package denys.mazurenko.prepdeckbackend.dto.auth.response;

/** The data of an "auth.resendTooSoon" refusal: seconds until another letter may be asked for. */
public record RetryAfterResponse(long retryAfterSeconds) {
}
