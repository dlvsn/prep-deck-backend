package denys.mazurenko.prepdeckbackend.dto.share.response;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A person with access to part of a tree: the topics granted to them (each with its subtree), in
 * tree order. {@code viaLink} is true when every one of them came through the public link.
 */
public record ShareMemberResponse(UserResponse user, TopicRole role, boolean viaLink, List<UUID> topicIds,
                                  Instant since) {
}
