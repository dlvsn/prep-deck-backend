package denys.mazurenko.prepdeckbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** How one-time email codes behave. */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.verification")
public class VerificationProperties {

    /** How long a code works after its letter is sent. */
    private Duration codeTtl;

    /** Wrong guesses allowed per code; the last one burns it. */
    private int maxAttempts;

    /** The least time between two letters to the same person. */
    private Duration resendCooldown;
}
