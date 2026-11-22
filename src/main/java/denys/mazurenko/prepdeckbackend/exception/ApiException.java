package denys.mazurenko.prepdeckbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A refusal the client should show: an HTTP status and a message key ("auth.emailTaken") that the
 * client translates into the user's language.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String messageKey) {
        super(messageKey);
        this.status = status;
    }

    public static ApiException sessionExpired() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "errors.sessionExpired");
    }
}
