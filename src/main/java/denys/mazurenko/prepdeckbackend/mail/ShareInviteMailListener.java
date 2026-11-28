package denys.mazurenko.prepdeckbackend.mail;

import denys.mazurenko.prepdeckbackend.config.AppMailProperties;
import denys.mazurenko.prepdeckbackend.config.EmailTemplateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sends the invitation letter once the invitation is safely in the database, on a thread of its
 * own. A failed send is logged; the owner can invite the address again after the cooldown.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShareInviteMailListener {

    private static final String PATH_SEPARATOR = " / ";

    private final EmailService emailService;
    private final AppMailProperties properties;
    private final EmailTemplateProperties templates;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInviteIssued(ShareInviteIssued event) {
        try {
            emailService.send(event.email(), templates.getInvite(), event.language(), Map.of(
                    "inviter_name", event.inviterName(),
                    "inviter_initial", initialOf(event.inviterName()),
                    "inviter_email", event.inviterEmail(),
                    "email", event.email(),
                    "invite_url", event.inviteUrl(),
                    "support_email", properties.getSupportEmail(),
                    "cards_total", Long.toString(event.cardsTotal()),
                    "topics", event.topics().stream().map(ShareInviteMailListener::row).toList()));
        } catch (Exception e) {
            log.error("Could not send the invitation letter to {}", event.email(), e);
        }
    }

    private static Map<String, Object> row(InvitedTopic topic) {
        return Map.of(
                "name", topic.title(),
                "path", pathOf(topic.path()),
                "cards_count", Long.toString(topic.cardCount()),
                "all_subtopics", topic.withSubtopics());
    }

    /** "Java / Core /" in front of the title; nothing for a root. */
    private static String pathOf(List<String> titles) {
        return titles.isEmpty() ? "" : String.join(PATH_SEPARATOR, titles) + PATH_SEPARATOR.stripTrailing();
    }

    private static String initialOf(String name) {
        String trimmed = name.strip();
        return trimmed.isEmpty() ? "?" : trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }
}
