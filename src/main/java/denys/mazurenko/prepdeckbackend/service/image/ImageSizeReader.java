package denys.mazurenko.prepdeckbackend.service.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.gif.GifHeaderDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;
import denys.mazurenko.prepdeckbackend.domain.ImageType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Reads a picture's width and height from its own headers (metadata-extractor), never decoding
 * the pixels, so the size the client claims is never needed or trusted.
 */
@Component
public class ImageSizeReader {

    /** EXIF orientations 5 to 8 store the picture turned by 90°: displayed, width and height swap. */
    private static final int FIRST_TURNED_ORIENTATION = 5;

    /**
     * The size as displayed, or empty when the headers do not give one (a damaged or forged file).
     * {@code length} is the exact number of bytes in {@code content}.
     */
    public Optional<ImageSize> read(InputStream content, long length, ImageType type) {
        Metadata metadata;
        try {
            metadata = ImageMetadataReader.readMetadata(content, length);
        } catch (ImageProcessingException | IOException e) {
            return Optional.empty();
        }
        Optional<ImageSize> stored = switch (type) {
            case PNG -> sizeIn(metadata, PngDirectory.class, PngDirectory.TAG_IMAGE_WIDTH, PngDirectory.TAG_IMAGE_HEIGHT);
            case JPEG -> sizeIn(metadata, JpegDirectory.class, JpegDirectory.TAG_IMAGE_WIDTH, JpegDirectory.TAG_IMAGE_HEIGHT);
            case GIF -> sizeIn(metadata, GifHeaderDirectory.class, GifHeaderDirectory.TAG_IMAGE_WIDTH, GifHeaderDirectory.TAG_IMAGE_HEIGHT);
            case WEBP -> sizeIn(metadata, WebpDirectory.class, WebpDirectory.TAG_IMAGE_WIDTH, WebpDirectory.TAG_IMAGE_HEIGHT);
        };
        return stored.map(size -> turned(metadata) ? new ImageSize(size.height(), size.width()) : size);
    }

    /** The first directory of that kind holding both values; PNG has one directory per chunk. */
    private static Optional<ImageSize> sizeIn(Metadata metadata, Class<? extends Directory> kind, int widthTag, int heightTag) {
        for (Directory directory : metadata.getDirectoriesOfType(kind)) {
            Integer width = directory.getInteger(widthTag);
            Integer height = directory.getInteger(heightTag);
            if (width != null && height != null && width > 0 && height > 0) {
                return Optional.of(new ImageSize(width, height));
            }
        }
        return Optional.empty();
    }

    private static boolean turned(Metadata metadata) {
        ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        Integer orientation = exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        return orientation != null && orientation >= FIRST_TURNED_ORIENTATION;
    }
}
