package denys.mazurenko.prepdeckbackend.dto.auth.response;

import java.util.UUID;

/** The user as the client sees them (client type `User`). */
public record UserResponse(UUID id, String username, String email, String name) {
}
