package denys.mazurenko.prepdeckbackend.dto.auth.response;

/** The data of an "auth.codeInvalid" refusal: wrong guesses still allowed for this code. */
public record CodeAttemptsResponse(int attemptsLeft) {
}
