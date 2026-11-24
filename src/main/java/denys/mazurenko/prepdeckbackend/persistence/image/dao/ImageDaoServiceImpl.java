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
    public List<Image> findOrphans(Instant before) {
        return imageRepository.findOrphans(before);
    }

    @Override
    @Transactional
    public Image save(Image image) {
        return imageRepository.save(image);
    }

    @Override
    @Transactional
    public void deleteAll(Collection<Image> images) {
        imageRepository.deleteAll(images);
    }
}
