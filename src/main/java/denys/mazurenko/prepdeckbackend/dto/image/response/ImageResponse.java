package denys.mazurenko.prepdeckbackend.dto.image.response;

import java.util.UUID;

/** An uploaded picture (client type `CardImage`); {@code url} is absolute and opens without a token. */
public record ImageResponse(UUID id, String url, int width, int height) {
}
