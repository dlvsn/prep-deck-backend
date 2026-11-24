package denys.mazurenko.prepdeckbackend.persistence.card.mapper;

import denys.mazurenko.prepdeckbackend.config.MapStructConfig;
import denys.mazurenko.prepdeckbackend.dto.card.response.CardResponse;
import denys.mazurenko.prepdeckbackend.persistence.card.entity.Card;
import denys.mazurenko.prepdeckbackend.persistence.image.mapper.ImageMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Practice is not built yet: every card is new for everyone and never due. */
@Mapper(config = MapStructConfig.class, uses = ImageMapper.class)
public interface CardMapper {

    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "dueAt", ignore = true)
    CardResponse toResponse(Card card);
}
