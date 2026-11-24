package denys.mazurenko.prepdeckbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Limits on a card's content that are a product decision rather than a column size. */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.cards")
public class CardProperties {

    /** Tags one card may carry. */
    private int maxTags;
}
