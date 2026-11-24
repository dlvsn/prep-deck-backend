package denys.mazurenko.prepdeckbackend.dto.topic.response;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;

import java.util.UUID;

/**
 * One topic of the flat list the client builds its tree from (client type `Topic`).
 * {@code cardCount} counts the cards directly in it; the client sums up subtrees. Sharing and
 * practice are not built yet: {@code role} is always owner, {@code shared} and {@code unseen}
 * false, {@code dueCount} 0.
 */
public record TopicResponse(UUID id, UUID parentId, String title, int order, UserResponse owner, TopicRole role,
                            boolean shared, long cardCount, long dueCount, boolean unseen) {
}
