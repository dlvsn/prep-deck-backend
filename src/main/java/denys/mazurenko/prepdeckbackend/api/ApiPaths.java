package denys.mazurenko.prepdeckbackend.api;

/**
 * Every path prefix, named once for the controllers and the security rules alike. They are
 * relative to the servlet context path ({@code server.servlet.context-path=/api}), so
 * {@link #AUTH} answers at {@code /api/auth}.
 */
public final class ApiPaths {

    public static final String AUTH = "/auth";
    public static final String SETTINGS = "/settings";
    public static final String TOPICS = "/topics";
    public static final String CARDS = "/cards";
    public static final String IMAGES = "/images";
    /** Below {@link #IMAGES}: a picture's bytes, open to anyone holding its id. */
    public static final String IMAGE_CONTENT = "/{id}/content";

    private ApiPaths() {
    }
}
