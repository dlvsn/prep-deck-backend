package denys.mazurenko.prepdeckbackend.persistence.topic.mapper;

import denys.mazurenko.prepdeckbackend.config.MapStructConfig;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Topics are only ever their owner's while sharing is not built: the role is owner, nothing is
 * shared or unseen. Practice is not built either, so nothing is due.
 */
@Mapper(config = MapStructConfig.class)
public interface TopicMapper {

    @Mapping(target = "id", source = "topic.id")
    @Mapping(target = "parentId", source = "topic.parentId")
    @Mapping(target = "title", source = "topic.title")
    @Mapping(target = "order", source = "topic.position")
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "role", constant = "OWNER")
    @Mapping(target = "shared", constant = "false")
    @Mapping(target = "cardCount", source = "cardCount")
    @Mapping(target = "dueCount", constant = "0L")
    @Mapping(target = "unseen", constant = "false")
    TopicResponse toResponse(Topic topic, UserResponse owner, long cardCount);
}
