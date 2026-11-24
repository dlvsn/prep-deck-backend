package denys.mazurenko.prepdeckbackend.persistence.image.mapper;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Where a picture can be fetched: an absolute URL on the host the request came to, so an
 * {@code <img>} can load it straight away. It needs no token; the id is random and unguessable.
 */
@Component
public class ImageUrls {

    @Named("imageUrl")
    public String of(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ApiPaths.IMAGES + ApiPaths.IMAGE_CONTENT)
                .buildAndExpand(id)
                .toUriString();
    }
}
