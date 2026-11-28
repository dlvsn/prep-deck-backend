package denys.mazurenko.prepdeckbackend.mail;

import java.util.List;

/**
 * One topic of an invitation letter: its title, the titles above it ({@code path}, empty for a
 * root), the cards in its whole subtree, and whether it has subtopics that come along.
 */
public record InvitedTopic(String title, List<String> path, long cardCount, boolean withSubtopics) {
}
