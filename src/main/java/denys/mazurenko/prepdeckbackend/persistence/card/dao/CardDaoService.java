package denys.mazurenko.prepdeckbackend.persistence.card.dao;

import denys.mazurenko.prepdeckbackend.persistence.card.entity.Card;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CardDaoService {

    Optional<Card> findById(UUID id);

    /** Newest first. */
    List<Card> findByTopic(UUID topicId);

    /** Cards directly in each topic; a topic without cards is missing from the map. */
    Map<UUID, Long> countByTopic(Collection<UUID> topicIds);

    List<String> findTagsOfOwner(UUID ownerId);

    Card save(Card card);

    void delete(Card card);
}
