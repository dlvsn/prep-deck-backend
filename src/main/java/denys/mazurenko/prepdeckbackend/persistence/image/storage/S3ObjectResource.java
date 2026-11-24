package denys.mazurenko.prepdeckbackend.persistence.image.storage;

import denys.mazurenko.prepdeckbackend.exception.ApiException;
import org.springframework.core.io.AbstractResource;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.InputStream;

/**
 * An S3 object served as a response body. Nothing is requested from S3 until the response is
 * actually written: {@link #getInputStream()} opens the object, and Spring's resource converter,
 * the only caller, closes the stream in a {@code finally} once the bytes are out. A response that
 * is never written (406, an exception on the way) therefore never holds a pooled S3 connection.
 *
 * <p>The size comes from the database, so nothing here reads the stream just to measure it
 * ({@code AbstractResource.contentLength()} would download the whole object to count it).
 */
public class S3ObjectResource extends AbstractResource {

    private final S3Client s3;
    private final String bucket;
    private final String key;
    private final long size;

    public S3ObjectResource(S3Client s3, String bucket, String key, long size) {
        this.s3 = s3;
        this.bucket = bucket;
        this.key = key;
        this.size = size;
    }

    /**
     * Opens the object. A key the database knows but the bucket lacks is a 404, raised before the
     * first byte is written, so the error still reaches the client as an ApiResponse.
     */
    @Override
    public InputStream getInputStream() {
        try {
            ResponseInputStream<GetObjectResponse> object = s3.getObject(request -> request.bucket(bucket).key(key));
            return new S3ObjectInputStream(object);
        } catch (NoSuchKeyException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "errors.notFound");
        }
    }

    /** Checked against the database by the caller; asking S3 again would cost a request. */
    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public long contentLength() {
        return size;
    }

    @Override
    public String getDescription() {
        return "S3 object [" + bucket + "/" + key + "]";
    }
}
