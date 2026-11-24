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
     * Keeps the file and answers its id and URL. The width and height come from the client, which
     * has decoded (and maybe scaled down) the picture already. Refuses "images.tooLarge" (413) and
     * "images.notAnImage" (415) for anything but PNG, JPEG, WebP or GIF.
     */
    ImageResponse upload(UUID userId, MultipartFile file, int width, int height);

    /** The bytes of a picture; "errors.notFound" when there is no such image. */
    ImageContent content(UUID imageId);

    /** Removes uploads older than the orphan TTL that no card refers to; answers how many. */
    int deleteOrphans();
}
