package denys.mazurenko.prepdeckbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A refusal the client should show: an HTTP status and a message key ("auth.emailTaken") that the
 * client translates into the user's language, plus, when the client needs more to act on it,
 * a small {@code data} object ("2 attempts left", the email to confirm).
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Object data;

    public ApiException(HttpStatus status, String messageKey) {
        this(status, messageKey, null);
    }

    public ApiException(HttpStatus status, String messageKey, Object data) {
        super(messageKey);
        this.status = status;
        this.data = data;
    }

    public static ApiException sessionExpired() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "errors.sessionExpired");
    }
}
