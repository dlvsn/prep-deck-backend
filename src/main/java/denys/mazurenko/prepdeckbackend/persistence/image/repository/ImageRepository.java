package denys.mazurenko.prepdeckbackend.persistence.image.repository;

import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    /** Uploads older than the cut-off that no card refers to. */
    @Query("""
            select i.id from Image i
            where i.createdAt < :before
              and not exists (select 1 from Card c join c.images ci where ci.id = i.id)
            """)
    List<UUID> findOrphanIds(@Param("before") Instant before);

    /**
     * Locks the row against every other writer, foreign-key checks included: a card being saved
     * with this image waits for the lock (or this waits for that card to commit). JPA's
     * PESSIMISTIC_WRITE is not enough here, since on PostgreSQL it becomes FOR NO KEY UPDATE,
     * which a foreign-key check does not wait for.
     */
    @Query(value = "select id from prep_deck.images where id = :id for update", nativeQuery = true)
    Optional<UUID> lockById(@Param("id") UUID id);

    @Query("select count(c) from Card c join c.images ci where ci.id = :id")
    long countCardsWith(@Param("id") UUID id);

    @Modifying
    @Query("delete from Image i where i.id = :id")
    int deleteRow(@Param("id") UUID id);
}
