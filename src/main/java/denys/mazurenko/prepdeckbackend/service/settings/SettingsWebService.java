package denys.mazurenko.prepdeckbackend.service.settings;

import denys.mazurenko.prepdeckbackend.dto.settings.request.UpdateSettingsRequest;
import denys.mazurenko.prepdeckbackend.dto.settings.response.SettingsResponse;

import java.util.UUID;

/**
 * A user's picks kept on the server, so every device they sign in on starts the same: theme,
 * accent, language, the topic opened last.
 */
public interface SettingsWebService {

    /** What is saved, or {@link SettingsResponse#nothingSaved()} before the first save. */
    SettingsResponse get(UUID userId);

    /** Replaces the whole set. */
    SettingsResponse update(UUID userId, UpdateSettingsRequest request);
}
