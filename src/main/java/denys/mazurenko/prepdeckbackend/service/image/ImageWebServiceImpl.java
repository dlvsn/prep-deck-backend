package denys.mazurenko.prepdeckbackend.service.image;

import denys.mazurenko.prepdeckbackend.config.ImageProperties;
import denys.mazurenko.prepdeckbackend.domain.ImageType;
import denys.mazurenko.prepdeckbackend.dto.image.response.ImageResponse;
import denys.mazurenko.prepdeckbackend.dto.image.response.MinImageSizeResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.image.dao.ImageDaoService;
import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import denys.mazurenko.prepdeckbackend.persistence.image.mapper.ImageMapper;
import denys.mazurenko.prepdeckbackend.persistence.image.storage.ImageFileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageWebServiceImpl implements ImageWebService {

    /** Enough of the file to tell the formats apart (WebP needs bytes 8 to 11). */
    private static final int SNIFF_BYTES = 12;

    private final ImageDaoService images;
    private final ImageFileStore files;
    private final ImageSizeReader sizeReader;
    private final ImageMapper imageMapper;
    private final ImageProperties properties;
    private final Clock clock;

    /**
     * The object goes up first, outside any transaction, so a slow upload to the bucket never
     * holds a database connection; the row is a short insert afterwards. Whatever fails on the
     * way, the object is taken back out, so nothing is left in the store without a row.
     */
    @Override
    public ImageResponse upload(UUID userId, MultipartFile file) {
        // The bytes actually received, counted by the servlet container (which also stops reading
        // past spring.servlet.multipart.max-file-size); nothing the client declared.
        if (file.getSize() > properties.getMaxSize().toBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "images.tooLarge");
        }
        ImageType type = ImageType.sniff(head(file))
                .orElseThrow(ImageWebServiceImpl::notAnImage);
        ImageSize size = sizeOf(file, type).orElseThrow(ImageWebServiceImpl::notAnImage);
        if (size.width() < properties.getMinWidth() || size.height() < properties.getMinHeight()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "images.tooSmall",
                    new MinImageSizeResponse(properties.getMinWidth(), properties.getMinHeight()));
        }

        UUID id = UUID.randomUUID();
        try (InputStream in = file.getInputStream()) {
            files.write(id, in, file.getSize(), type);
        } catch (IOException e) {
            removeQuietly(id);
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            removeQuietly(id);
            throw e;
        }

        Image image = new Image();
        image.setId(id);
        image.setOwnerId(userId);
        image.setContentType(type);
        image.setSizeBytes(file.getSize());
        image.setWidth(size.width());
        image.setHeight(size.height());
        Image saved;
        try {
            saved = images.insert(image);
        } catch (RuntimeException e) {
            removeQuietly(id);
            throw e;
        }
        return imageMapper.toResponse(saved);
    }

    @Override
    public ImageContent content(UUID imageId) {
        Image image = images.findById(imageId).orElseThrow(ImageWebServiceImpl::notFound);
        return files.read(imageId, image.getSizeBytes())
                .map(resource -> new ImageContent(resource, image.getContentType(), image.getSizeBytes()))
                .orElseThrow(ImageWebServiceImpl::notFound);
    }

    /**
     * One orphan at a time, each row in its own short transaction and its object after it, outside
     * any. A row is deleted only if it is still unused at that moment, so an upload a card took
     * after the search is kept (deleting it would cascade it off that card).
     */
    @Override
    public int deleteOrphans() {
        List<UUID> candidates = images.findOrphanIds(clock.instant().minus(properties.getOrphanTtl()));
        int removed = 0;
        for (UUID id : candidates) {
            if (images.deleteIfOrphan(id)) {
                removed++;
                removeQuietly(id);
            }
        }
        return removed;
    }

    /** Takes an object out of the store; a failure is logged with the id, so it can be swept by hand. */
    private void removeQuietly(UUID id) {
        try {
            files.delete(id);
        } catch (RuntimeException e) {
            log.error("Could not delete stored image {}; it has no row and must be removed by hand", id, e);
        }
    }

    private static byte[] head(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(SNIFF_BYTES);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<ImageSize> sizeOf(MultipartFile file, ImageType type) {
        try (InputStream in = file.getInputStream()) {
            return sizeReader.read(in, file.getSize(), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ApiException notAnImage() {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "images.notAnImage");
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "errors.notFound");
    }
}
