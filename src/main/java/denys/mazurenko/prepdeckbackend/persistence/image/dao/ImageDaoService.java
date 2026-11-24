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

    List<Image> findOrphans(Instant before);

    Image save(Image image);

    void deleteAll(Collection<Image> images);
}
