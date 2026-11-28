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
    /** Below {@link #TOPICS}: who can see the tree a topic belongs to. */
    public static final String TOPIC_SHARING = "/{id}/sharing";
    /** Topics other people shared with the caller. */
    public static final String SHARES = "/shares";
    public static final String CARDS = "/cards";
    public static final String IMAGES = "/images";
    /** Below {@link #IMAGES}: a picture's bytes, open to anyone holding its id. */
    public static final String IMAGE_CONTENT = "/{id}/content";

    private ApiPaths() {
    }
}
