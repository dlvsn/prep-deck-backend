package denys.mazurenko.prepdeckbackend.persistence.image.storage;

import denys.mazurenko.prepdeckbackend.config.ImageProperties;
import denys.mazurenko.prepdeckbackend.domain.ImageType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * Files in {@code prep-deck.images.directory}, made on start if missing. Used when
 * {@code prep-deck.images.storage=LOCAL}.
 */
@Component
@ConditionalOnProperty(name = "prep-deck.images.storage", havingValue = "LOCAL")
@RequiredArgsConstructor
public class DiskImageFileStoreImpl implements ImageFileStore {

    private final ImageProperties properties;

    @PostConstruct
    void createDirectory() throws IOException {
        Files.createDirectories(properties.getDirectory());
    }

    @Override
    public void write(UUID id, InputStream content, long size, ImageType type) {
        try {
            Files.copy(content, pathOf(id), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<Resource> read(UUID id) {
        Path path = pathOf(id);
        return Files.isRegularFile(path) ? Optional.of(new FileSystemResource(path)) : Optional.empty();
    }

    @Override
    public void delete(UUID id) {
        try {
            Files.deleteIfExists(pathOf(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The id is a UUID, so the name can never climb out of the folder. */
    private Path pathOf(UUID id) {
        return properties.getDirectory().resolve(id.toString());
    }
}
