package denys.mazurenko.prepdeckbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Sharing topics with other people: what links look like, how invitations behave. */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.sharing")
public class SharingProperties {

    /**
     * What a link is made of: this, then the token as it is ("prepdeck://topic/" gives
     * "prepdeck://topic/k3J9…"). Public links and the links in invitation letters look the same.
     */
    private String linkBaseUrl;

    /** How long an invitation letter can be accepted. */
    private Duration inviteTtl;

    /** The least time between two letters from one owner to one address. */
    private Duration inviteCooldown;

    /** The most cards one "copy to my topics" may take, so a single request stays bounded. */
    private int maxCopyCards;
}
