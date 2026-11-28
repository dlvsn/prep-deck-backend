package denys.mazurenko.prepdeckbackend.service.share;

import java.util.List;
import java.util.UUID;

/** A card read for copying: its content, and its pictures by the ids of the originals. */
record CardCopy(String question, String answer, List<String> tags, List<UUID> imageIds) {
}
