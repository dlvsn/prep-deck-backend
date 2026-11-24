package denys.mazurenko.prepdeckbackend.persistence.card.repository;

import java.util.UUID;

/** How many cards sit directly in one topic. */
public interface TopicCardCount {

    UUID getTopicId();

    long getTotal();
}
