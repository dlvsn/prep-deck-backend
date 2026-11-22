package denys.mazurenko.prepdeckbackend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 401 and 403 from the security filters, in the same ApiResponse shape as everything else. These
 * happen before any controller, so GlobalExceptionHandler never sees them.
 *
 * <p>A 401 always says "errors.sessionExpired": the client answers it by refreshing once and, if
 * that fails too, by showing the login screen, whatever the precise reason was.
 */
@Component
public class JsonSecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, "errors.sessionExpired");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "errors.forbidden");
    }

    /** Message keys are plain identifiers, so the body needs no JSON escaping. */
    private static void write(HttpServletResponse response, HttpStatus status, String messageKey) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"message\":\"" + messageKey + "\",\"data\":null}");
    }
}
