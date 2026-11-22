package denys.mazurenko.prepdeckbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Our own mail settings; the SMTP server itself is Spring Boot's `spring.mail.*`. */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.mail")
public class AppMailProperties {

    /** The sender of every letter, e.g. "Prep Deck <no-reply@prepdeck.app>". */
    private String from;

    /** Shown in letters as the address to write to for help. */
    private String supportEmail;
}
