package denys.mazurenko.prepdeckbackend.dto.share.request;

import java.util.UUID;

/** Where the copy goes among the caller's own topics; parentId null makes it a root. */
public record CopyTopicRequest(UUID parentId) {
}
