package denys.mazurenko.prepdeckbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.cors")
public class CorsProperties {

    /** The paths CORS applies to, relative to the context path, e.g. "/**" for every endpoint. */
    private String mapping;

    /** Browser origins allowed to call the API: the client's dev server, later the web app. */
    private List<String> allowedOrigins = new ArrayList<>();
}
