package denys.mazurenko.prepdeckbackend.persistence.image.mapper;

import denys.mazurenko.prepdeckbackend.config.MapStructConfig;
import denys.mazurenko.prepdeckbackend.dto.image.response.ImageResponse;
import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class, uses = ImageUrls.class)
public interface ImageMapper {

    @Mapping(target = "url", source = "id", qualifiedByName = "imageUrl")
    ImageResponse toResponse(Image image);
}
