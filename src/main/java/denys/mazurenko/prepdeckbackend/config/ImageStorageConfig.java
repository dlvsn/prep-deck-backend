package denys.mazurenko.prepdeckbackend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.stream.Stream;

/** The S3 client, made only when pictures are kept in a bucket ({@code prep-deck.images.storage=S3}). */
@Configuration
@ConditionalOnProperty(name = "prep-deck.images.storage", havingValue = "S3")
public class ImageStorageConfig {

    /**
     * Path-style addressing ({@code endpoint/bucket/key}) works with every S3-compatible service.
     * Checksums are sent only where the API demands them: the SDK's newer defaults add headers that
     * not every S3-compatible service accepts.
     */
    @Bean
    public S3Client imageS3Client(ImageS3Properties properties) {
        boolean incomplete = Stream.of(
                        properties.getEndpoint(), properties.getRegion(), properties.getBucket(),
                        properties.getAccessKeyId(), properties.getSecretAccessKey())
                .anyMatch(value -> !StringUtils.hasText(value));
        if (incomplete) {
            throw new IllegalStateException("IMAGES_STORAGE=S3 needs S3_ENDPOINT, S3_REGION, S3_BUCKET, "
                    + "S3_ACCESS_KEY_ID and S3_SECRET_ACCESS_KEY in .env");
        }
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint().trim()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getSecretAccessKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
