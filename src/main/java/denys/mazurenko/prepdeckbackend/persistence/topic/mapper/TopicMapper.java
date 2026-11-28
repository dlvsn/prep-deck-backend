package denys.mazurenko.prepdeckbackend.persistence.topic.mapper;

import denys.mazurenko.prepdeckbackend.config.MapStructConfig;
import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

/** Practice is not built yet, so nothing is due. */
@Mapper(config = MapStructConfig.class)
public interface TopicMapper {

    /** A topic in its owner's own tree. */
    @Mapping(target = "id", source = "topic.id")
    @Mapping(target = "parentId", source = "topic.parentId")
    @Mapping(target = "title", source = "topic.title")
    @Mapping(target = "order", source = "topic.position")
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "role", constant = "OWNER")
    @Mapping(target = "shared", source = "shared")
    @Mapping(target = "cardCount", source = "cardCount")
    @Mapping(target = "dueCount", constant = "0L")
    @Mapping(target = "unseen", constant = "false")
    @Mapping(target = "path", expression = "java(java.util.List.of())")
    TopicResponse toOwnResponse(Topic topic, UserResponse owner, long cardCount, boolean shared);

    /**
     * A topic somebody shared with the caller; {@code parentId} is null where the tree they can
     * see starts, and {@code path} names the parents above it they cannot see.
     */
    @Mapping(target = "id", source = "topic.id")
    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "title", source = "topic.title")
    @Mapping(target = "order", source = "topic.position")
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "shared", constant = "false")
    @Mapping(target = "cardCount", source = "cardCount")
    @Mapping(target = "dueCount", constant = "0L")
    @Mapping(target = "unseen", source = "unseen")
    @Mapping(target = "path", source = "path")
    TopicResponse toSharedResponse(Topic topic, UUID parentId, UserResponse owner, TopicRole role, long cardCount,
                                   boolean unseen, List<String> path);
}
