package denys.mazurenko.prepdeckbackend.config;

import denys.mazurenko.prepdeckbackend.domain.Language;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the letter templates are and what they are called. A template's file is
 * {@code <location><name>-<language code><suffix>}, e.g. {@code classpath:templates/verify-email-uk.html}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.mail.templates")
public class EmailTemplateProperties {

    /** A Spring resource location ending in "/", e.g. "classpath:templates/". */
    private String location;

    private String suffix;

    /** Used when a template has no file in the requested language. */
    private Language fallbackLanguage;

    /** The template of the sign-up confirmation letter. */
    private String verifyEmail;

    /** The template of the password reset letter. */
    private String resetPassword;

    /** The template of the letter inviting someone to topics shared with them. */
    private String invite;
}
