package denys.mazurenko.prepdeckbackend.persistence.image.storage;

import denys.mazurenko.prepdeckbackend.domain.ImageType;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * The bytes of uploaded pictures, one object per image id; their rows live in the database.
 * {@code prep-deck.images.storage} picks the implementation: a local folder or an S3 bucket.
 */
public interface ImageFileStore {

    /** {@code size} is the exact number of bytes {@code content} holds. */
    void write(UUID id, InputStream content, long size, ImageType type);

    Optional<Resource> read(UUID id);

    /** Nothing happens when the file is already gone. */
    void delete(UUID id);
}
