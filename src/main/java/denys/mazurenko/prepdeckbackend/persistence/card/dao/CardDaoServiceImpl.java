package denys.mazurenko.prepdeckbackend.persistence.card.dao;

import denys.mazurenko.prepdeckbackend.persistence.card.entity.Card;
import denys.mazurenko.prepdeckbackend.persistence.card.repository.CardRepository;
import denys.mazurenko.prepdeckbackend.persistence.card.repository.TopicCardCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardDaoServiceImpl implements CardDaoService {

    private final CardRepository cardRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Card> findById(UUID id) {
        return cardRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> findByTopic(UUID topicId) {
        return cardRepository.findByTopicIdOrderByCreatedAtDescIdAsc(topicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> countByTopic(Collection<UUID> topicIds) {
        if (topicIds.isEmpty()) {
            return Map.of();
        }
        return cardRepository.countByTopic(topicIds).stream()
                .collect(Collectors.toMap(TopicCardCount::getTopicId, TopicCardCount::getTotal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findTagsOfOwner(UUID ownerId) {
        return cardRepository.findTagsOfOwner(ownerId);
    }

    @Override
    @Transactional
    public Card save(Card card) {
        return cardRepository.save(card);
    }

    @Override
    @Transactional
    public void delete(Card card) {
        cardRepository.delete(card);
    }
}
