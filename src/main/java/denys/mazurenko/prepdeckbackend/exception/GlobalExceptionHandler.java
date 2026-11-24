package denys.mazurenko.prepdeckbackend.exception;

import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every failure into an {@link ApiResponse}, so a client never has to parse a second error
 * format. The HTTP status still tells the truth; the body adds a message key the client translates.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage(), e.getData()));
    }

    /** Bean Validation: the first broken rule, whose message is already a key ("auth.passwordRules"). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(MethodArgumentNotValidException e) {
        String key = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("errors.badRequest");
        return ResponseEntity.badRequest().body(ApiResponse.error(key));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException e) {
        String message = e.getReason() != null ? e.getReason() : "errors.generic";
        return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error(message));
    }

    /** Spring MVC's own refusals keep their status (404, 405, 415, 400) instead of becoming a 500. */
    @ExceptionHandler({
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMvc(Exception e) {
        HttpStatusCode status = e instanceof ErrorResponse er ? er.getStatusCode() : HttpStatus.BAD_REQUEST;
        String key = status.value() == 404 ? "errors.notFound" : "errors.badRequest";
        return ResponseEntity.status(status).body(ApiResponse.error(key));
    }

    /** A file past spring.servlet.multipart.max-file-size, refused before any controller runs. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.error("images.tooLarge"));
    }

    /** Anything unforeseen: logged in full here, never leaked to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("errors.generic"));
    }
}
