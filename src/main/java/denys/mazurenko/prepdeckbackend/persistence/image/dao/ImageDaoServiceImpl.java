package denys.mazurenko.prepdeckbackend.persistence.image.dao;

import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import denys.mazurenko.prepdeckbackend.persistence.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageDaoServiceImpl implements ImageDaoService {

    private final ImageRepository imageRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Image> findById(UUID id) {
        return imageRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Image> findAllById(Collection<UUID> ids) {
        return imageRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findOrphanIds(Instant before) {
        return imageRepository.findOrphanIds(before);
    }

    @Override
    @Transactional
    public Image insert(Image image) {
        if (!image.isNew()) {
            throw new IllegalArgumentException("Image " + image.getId() + " is already stored");
        }
        return imageRepository.save(image);
    }

    /**
     * Lock first, then look: under READ COMMITTED each statement sees what was committed before it
     * started, so the check after the lock sees a card that committed while we waited for it.
     */
    @Override
    @Transactional
    public boolean deleteIfOrphan(UUID id) {
        if (imageRepository.lockById(id).isEmpty() || imageRepository.countCardsWith(id) > 0) {
            return false;
        }
        return imageRepository.deleteRow(id) == 1;
    }
}
