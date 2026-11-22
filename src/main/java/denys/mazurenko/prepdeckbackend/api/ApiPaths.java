package denys.mazurenko.prepdeckbackend.api;

/**
 * Every path prefix, named once for the controllers and the security rules alike. They are
 * relative to the servlet context path ({@code server.servlet.context-path=/api}), so
 * {@link #AUTH} answers at {@code /api/auth}.
 */
public final class ApiPaths {

    public static final String AUTH = "/auth";

    private ApiPaths() {
    }
}
