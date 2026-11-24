package denys.mazurenko.prepdeckbackend.service.image;

import denys.mazurenko.prepdeckbackend.config.ImageProperties;
import denys.mazurenko.prepdeckbackend.domain.ImageType;
import denys.mazurenko.prepdeckbackend.dto.image.response.ImageResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.image.dao.ImageDaoService;
import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import denys.mazurenko.prepdeckbackend.persistence.image.mapper.ImageMapper;
import denys.mazurenko.prepdeckbackend.persistence.image.storage.ImageFileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageWebServiceImpl implements ImageWebService {

    /** Enough of the file to tell the formats apart (WebP needs bytes 8 to 11). */
    private static final int SNIFF_BYTES = 12;
    private static final int MAX_DIMENSION = 20_000;

    private final ImageDaoService images;
    private final ImageFileStore files;
    private final ImageMapper imageMapper;
    private final ImageProperties properties;
    private final Clock clock;

    /** The row and the file go together: a file that cannot be written rolls the row back. */
    @Override
    @Transactional
    public ImageResponse upload(UUID userId, MultipartFile file, int width, int height) {
        if (file.getSize() > properties.getMaxSize().toBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "images.tooLarge");
        }
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "errors.badRequest");
        }
        ImageType type = ImageType.sniff(head(file))
                .orElseThrow(() -> new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "images.notAnImage"));

        Image image = new Image();
        image.setOwnerId(userId);
        image.setContentType(type);
        image.setSizeBytes(file.getSize());
        image.setWidth(width);
        image.setHeight(height);
        Image saved = images.save(image);

        try (InputStream in = file.getInputStream()) {
            files.write(saved.getId(), in, file.getSize(), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return imageMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageContent content(UUID imageId) {
        Image image = images.findById(imageId).orElseThrow(ImageWebServiceImpl::notFound);
        return files.read(imageId)
                .map(resource -> new ImageContent(resource, image.getContentType(), image.getSizeBytes()))
                .orElseThrow(ImageWebServiceImpl::notFound);
    }

    /** Rows first; a file that will not go is only logged, it no longer belongs to anything. */
    @Override
    @Transactional
    public int deleteOrphans() {
        List<Image> orphans = images.findOrphans(clock.instant().minus(properties.getOrphanTtl()));
        if (orphans.isEmpty()) {
            return 0;
        }
        images.deleteAll(orphans);
        for (Image orphan : orphans) {
            try {
                files.delete(orphan.getId());
            } catch (UncheckedIOException e) {
                log.warn("Could not delete the file of unused image {}", orphan.getId(), e);
            }
        }
        return orphans.size();
    }

    private static byte[] head(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(SNIFF_BYTES);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "errors.notFound");
    }
}
