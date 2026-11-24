package denys.mazurenko.prepdeckbackend.persistence.image.storage;

import software.amazon.awssdk.core.ResponseInputStream;

import java.io.FilterInputStream;
import java.io.IOException;

/**
 * The body of a GetObject response that gives its connection back properly however it ends.
 * Read to the end, close returns the connection to the pool. Closed earlier (the client went away
 * mid-picture), the HTTP client would otherwise read and discard the rest of the object to reuse
 * the connection; here the request is aborted instead and the connection dropped.
 */
public class S3ObjectInputStream extends FilterInputStream {

    private final ResponseInputStream<?> response;
    private boolean finished;

    public S3ObjectInputStream(ResponseInputStream<?> response) {
        super(response);
        this.response = response;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value == -1) {
            finished = true;
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int count = super.read(buffer, offset, length);
        if (count == -1) {
            finished = true;
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        if (!finished) {
            response.abort();
        }
        super.close();
    }
}
