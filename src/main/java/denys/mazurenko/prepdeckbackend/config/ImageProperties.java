package denys.mazurenko.prepdeckbackend.config;

import denys.mazurenko.prepdeckbackend.domain.ImageStorageType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.time.Duration;

/** Card pictures: where the files are kept, what is accepted, when unused uploads go. */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.images")
public class ImageProperties {

    /** Where the bytes go: a local folder ({@link #directory}) or an S3 bucket (ImageS3Properties). */
    private ImageStorageType storage;

    /** LOCAL: the folder uploaded files are written to, one file per image named by its id. */
    private Path directory;

    /** The largest file accepted, measured on the bytes received. */
    private DataSize maxSize;

    /** The narrowest picture accepted, in pixels as displayed. */
    private int minWidth;

    /** The lowest picture accepted, in pixels as displayed. */
    private int minHeight;

    /** Pictures one card may hold. */
    private int maxPerCard;

    /** How long an upload no card refers to is kept before it is removed. */
    private Duration orphanTtl;

    /** How often unused uploads are looked for. */
    private Duration cleanupInterval;
}
