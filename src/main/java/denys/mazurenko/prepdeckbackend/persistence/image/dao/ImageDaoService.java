package denys.mazurenko.prepdeckbackend.persistence.image.dao;

import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImageDaoService {

    Optional<Image> findById(UUID id);

    List<Image> findAllById(Collection<UUID> ids);

    /** Ids of uploads older than the cut-off that no card refers to right now. */
    List<UUID> findOrphanIds(Instant before);

    /** Inserts a new image whose id was chosen by the caller. */
    Image insert(Image image);

    /**
     * Deletes the row if no card refers to it at this moment, checked under a row lock so a card
     * saved concurrently with this image keeps it. Answers whether the row was deleted.
     */
    boolean deleteIfOrphan(UUID id);
}
