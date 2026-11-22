package denys.mazurenko.prepdeckbackend.mail;

import denys.mazurenko.prepdeckbackend.config.AppMailProperties;
import denys.mazurenko.prepdeckbackend.config.EmailTemplateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Sends the confirmation letter once the code is safely in the database, on a thread of its
 * own. A rolled-back registration sends nothing; a failed send is logged, and the person can ask
 * for the code again.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailListener {

    private final EmailService emailService;
    private final AppMailProperties properties;
    private final EmailTemplateProperties templates;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCodeIssued(VerificationCodeIssued event) {
        try {
            emailService.send(event.email(), templates.getVerifyEmail(), event.language(), Map.of(
                    "code", event.code(),
                    "username", event.username(),
                    "email", event.email(),
                    "expires_minutes", Long.toString(event.ttlMinutes()),
                    "support_email", properties.getSupportEmail()));
        } catch (Exception e) {
            log.error("Could not send the confirmation letter to {}", event.email(), e);
        }
    }
}
