package denys.mazurenko.prepdeckbackend.persistence.image.repository;

import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {

    /** Uploads older than the cut-off that no card refers to. */
    @Query("""
            select i from Image i
            where i.createdAt < :before
              and not exists (select 1 from Card c join c.images ci where ci.id = i.id)
            """)
    List<Image> findOrphans(@Param("before") Instant before);
}
