package denys.mazurenko.prepdeckbackend.persistence.image.storage;

import denys.mazurenko.prepdeckbackend.config.ImageS3Properties;
import denys.mazurenko.prepdeckbackend.domain.ImageType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Objects in an S3 bucket ({@code prep-deck.images.s3.*}), keyed {@code <key prefix><image id>}.
 * Used when {@code prep-deck.images.storage=S3}. Pictures are still served through this
 * application, so the bucket stays private and needs no public access.
 */
@Component
@ConditionalOnProperty(name = "prep-deck.images.storage", havingValue = "S3")
@RequiredArgsConstructor
public class S3ImageFileStoreImpl implements ImageFileStore {

    private final S3Client s3;
    private final ImageS3Properties properties;

    @Override
    public void write(UUID id, InputStream content, long size, ImageType type) {
        s3.putObject(request -> request
                        .bucket(properties.getBucket())
                        .key(keyOf(id))
                        .contentType(type.getMediaType())
                        .contentLength(size),
                RequestBody.fromInputStream(content, size));
    }

    /** The stream is the open HTTP response; Spring closes it once the bytes are sent. */
    @Override
    public Optional<Resource> read(UUID id) {
        try {
            InputStream object = s3.getObject(request -> request.bucket(properties.getBucket()).key(keyOf(id)));
            return Optional.of(new InputStreamResource(object));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    /** S3 answers a delete of a missing key with success too. */
    @Override
    public void delete(UUID id) {
        s3.deleteObject(request -> request.bucket(properties.getBucket()).key(keyOf(id)));
    }

    private String keyOf(UUID id) {
        return properties.getKeyPrefix() + id;
    }
}
