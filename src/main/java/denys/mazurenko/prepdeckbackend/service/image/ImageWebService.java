package denys.mazurenko.prepdeckbackend.service.image;

import denys.mazurenko.prepdeckbackend.dto.image.response.ImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Card pictures. They are uploaded on their own, before the card that shows them is saved; the
 * card then lists their ids. Uploads no card ever took are removed after a while.
 */
public interface ImageWebService {

    /**
     * Keeps the file and answers its id, URL and size. Everything is measured on the file itself:
     * its weight on the bytes received, its format on its first bytes, its width and height on its
     * headers. Refuses "images.tooLarge" (413), "images.notAnImage" (415: not PNG, JPEG, WebP or GIF,
     * or no readable size) and "images.tooSmall" (400, with the minimum in data).
     */
    ImageResponse upload(UUID userId, MultipartFile file);

    /** The bytes of a picture; "errors.notFound" when there is no such image. */
    ImageContent content(UUID imageId);

    /** Removes uploads older than the orphan TTL that no card refers to; answers how many. */
    int deleteOrphans();
}
