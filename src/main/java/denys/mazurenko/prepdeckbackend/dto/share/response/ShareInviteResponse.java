package denys.mazurenko.prepdeckbackend.dto.share.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** An invitation letter not accepted yet. */
public record ShareInviteResponse(UUID id, String email, List<UUID> topicIds, Instant invitedAt,
                                  Instant expiresAt) {
}
