package denys.mazurenko.prepdeckbackend.persistence.share.repository;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.TopicGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TopicGrantRepository extends JpaRepository<TopicGrant, UUID> {

    /**
     * The roles a user holds on a topic through grants on it or on any topic above it, walking up
     * the tree in one statement. The tree has no cycles (a move into its own subtree is refused),
     * so the walk ends at the root.
     */
    @Query(value = """
            with recursive chain (id, parent_id) as (
                select t.id, t.parent_id from prep_deck.topics t where t.id = :topicId
                union all
                select t.id, t.parent_id from prep_deck.topics t join chain c on t.id = c.parent_id
            )
            select distinct g.role
            from prep_deck.topic_grants g
            where g.grantee_id = :userId
              and g.topic_id in (select id from chain)
            """, nativeQuery = true)
    List<String> findRolesOnTopic(@Param("userId") UUID userId, @Param("topicId") UUID topicId);

    List<TopicGrant> findByGranteeId(UUID granteeId);

    List<TopicGrant> findByOwnerId(UUID ownerId);

    /** Does nothing when the user already holds a grant on exactly this topic. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into prep_deck.topic_grants (topic_id, owner_id, grantee_id, role, source)
            values (:topicId, :ownerId, :granteeId, :role, :source)
            on conflict (topic_id, grantee_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("topicId") UUID topicId, @Param("ownerId") UUID ownerId,
                       @Param("granteeId") UUID granteeId, @Param("role") String role,
                       @Param("source") String source);

    @Modifying(flushAutomatically = true)
    @Query("delete from TopicGrant g where g.id in :ids")
    int deleteByIds(@Param("ids") Collection<UUID> ids);

    @Modifying(flushAutomatically = true)
    @Query("""
            update TopicGrant g set g.seenAt = :now
            where g.granteeId = :granteeId and g.topicId in :topicIds and g.seenAt is null
            """)
    int markSeen(@Param("granteeId") UUID granteeId, @Param("topicIds") Collection<UUID> topicIds,
                 @Param("now") Instant now);
}
