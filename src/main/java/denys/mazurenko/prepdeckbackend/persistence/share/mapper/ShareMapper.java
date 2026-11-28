package denys.mazurenko.prepdeckbackend.persistence.share.mapper;

import denys.mazurenko.prepdeckbackend.config.MapStructConfig;
import denys.mazurenko.prepdeckbackend.dto.share.response.ShareInviteResponse;
import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareInvite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(config = MapStructConfig.class)
public interface ShareMapper {

    /** {@code topicIds} in tree order, which the entity's set does not keep. */
    @Mapping(target = "id", source = "invite.id")
    @Mapping(target = "email", source = "invite.email")
    @Mapping(target = "topicIds", source = "topicIds")
    @Mapping(target = "invitedAt", source = "invite.createdAt")
    @Mapping(target = "expiresAt", source = "invite.expiresAt")
    ShareInviteResponse toResponse(ShareInvite invite, List<UUID> topicIds);
}
