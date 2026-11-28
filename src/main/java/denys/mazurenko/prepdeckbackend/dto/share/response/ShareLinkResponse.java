package denys.mazurenko.prepdeckbackend.dto.share.response;

import java.util.List;
import java.util.UUID;

/**
 * The tree's public link. {@code token} and {@code url} are present only while it is enabled;
 * {@code joinedCount} counts the people who came through it alone.
 */
public record ShareLinkResponse(boolean enabled, String token, String url, List<UUID> topicIds, int joinedCount) {
}
