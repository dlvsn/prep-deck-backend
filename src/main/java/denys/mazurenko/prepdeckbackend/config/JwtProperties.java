package denys.mazurenko.prepdeckbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Access tokens are short-lived JWTs; refresh tokens are opaque and live in the database. */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.jwt")
public class JwtProperties {

    /** HMAC-SHA256 key, at least 32 bytes. Blank: a random key for this run only. */
    private String secret;

    private String issuer;

    private Duration accessTtl;

    private Duration refreshTtl;
}
