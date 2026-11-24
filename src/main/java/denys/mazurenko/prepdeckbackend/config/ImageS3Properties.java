package denys.mazurenko.prepdeckbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The S3 bucket card pictures go to when {@code prep-deck.images.storage=S3}. Any S3-compatible
 * service fits; for Cloudflare R2 the endpoint is {@code https://<account id>.r2.cloudflarestorage.com}
 * and the region {@code auto}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "prep-deck.images.s3")
public class ImageS3Properties {

    /** Kept as text: it is empty while storage is LOCAL. */
    private String endpoint;

    private String region;

    private String bucket;

    /** Prepended to the image id to make the object key, e.g. {@code images/}. */
    private String keyPrefix;

    private String accessKeyId;

    private String secretAccessKey;
}
