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

    /**
     * The picture as a response body, {@code size} bytes long (known from its row). Empty when the
     * store can tell cheaply that it is gone. Nothing may be opened here: the bytes are opened when
     * the response is written, by whoever writes it, and closed by it too.
     */
    Optional<Resource> read(UUID id, long size);

    /** Nothing happens when the file is already gone. */
    void delete(UUID id);
}
