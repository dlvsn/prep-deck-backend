package denys.mazurenko.prepdeckbackend.dto.card.response;

import denys.mazurenko.prepdeckbackend.domain.CardStatus;
import denys.mazurenko.prepdeckbackend.dto.image.response.ImageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A card as the client shows it (client type `Card`). {@code status} and {@code dueAt} are the
 * caller's own practice progress: until practice exists every card is new, never due.
 */
public record CardResponse(UUID id, UUID topicId, String question, String answer, List<String> tags,
                           List<ImageResponse> images, CardStatus status, Instant dueAt, Instant createdAt,
                           Instant updatedAt) {
}
