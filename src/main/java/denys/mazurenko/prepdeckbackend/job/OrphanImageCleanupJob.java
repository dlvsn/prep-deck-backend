package denys.mazurenko.prepdeckbackend.job;

import denys.mazurenko.prepdeckbackend.service.image.ImageWebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes pictures that were uploaded for a card that was never saved, or taken off every card,
 * once they are older than {@code prep-deck.images.orphan-ttl}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanImageCleanupJob {

    private final ImageWebService imageWebService;

    @Scheduled(initialDelayString = "${prep-deck.images.cleanup-interval}",
            fixedDelayString = "${prep-deck.images.cleanup-interval}")
    public void run() {
        int removed = imageWebService.deleteOrphans();
        if (removed > 0) {
            log.info("Removed {} unused image(s)", removed);
        }
    }
}
