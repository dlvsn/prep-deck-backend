package denys.mazurenko.prepdeckbackend.dto.topic.response;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;

import java.util.List;
import java.util.UUID;

/**
 * One topic of the flat list the client builds its tree from (client type `Topic`).
 * {@code cardCount} counts the cards directly in it; the client sums up subtrees.
 *
 * <p>In My topics {@code role} is owner and {@code shared} says whether anyone else can see the
 * topic or has been invited to. In Shared with me {@code parentId} is null for the topmost topic
 * the caller can see even when it has a parent, {@code path} names the parents they cannot see
 * ("Java" for a shared "Java / JVM"), and {@code unseen} marks a share not opened yet. Practice is
 * not built yet: {@code dueCount} is 0.
 */
public record TopicResponse(UUID id, UUID parentId, String title, int order, UserResponse owner, TopicRole role,
                            boolean shared, long cardCount, long dueCount, boolean unseen, List<String> path) {
}
