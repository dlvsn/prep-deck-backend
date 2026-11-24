package denys.mazurenko.prepdeckbackend.service.image;

import denys.mazurenko.prepdeckbackend.domain.ImageType;
import org.springframework.core.io.Resource;

/** A picture's bytes ready to stream, with what the response headers need. */
public record ImageContent(Resource resource, ImageType type, long size) {
}
