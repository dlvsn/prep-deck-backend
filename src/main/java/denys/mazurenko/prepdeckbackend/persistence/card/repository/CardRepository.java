package denys.mazurenko.prepdeckbackend.persistence.card.repository;

import denys.mazurenko.prepdeckbackend.persistence.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    /** Newest first: a card just made shows at the top of the grid. */
    List<Card> findByTopicIdOrderByCreatedAtDescIdAsc(UUID topicId);

    @Query("""
            select c.topicId as topicId, count(c) as total
            from Card c
            where c.topicId in :topicIds
            group by c.topicId
            """)
    List<TopicCardCount> countByTopic(@Param("topicIds") Collection<UUID> topicIds);

    /** Every tag on the owner's cards, for suggestions while a tag is typed. */
    @Query("""
            select distinct t
            from Card c join c.tags t
            where c.topicId in (select tp.id from Topic tp where tp.ownerId = :ownerId)
            order by t
            """)
    List<String> findTagsOfOwner(@Param("ownerId") UUID ownerId);
}
