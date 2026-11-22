package denys.mazurenko.prepdeckbackend.dto.settings.response;

import denys.mazurenko.prepdeckbackend.domain.AccentColor;
import denys.mazurenko.prepdeckbackend.domain.Language;
import denys.mazurenko.prepdeckbackend.domain.ThemeMode;

import java.time.Instant;

/**
 * A user's saved picks (client type `Settings`). {@code updatedAt} null: nothing saved yet, every
 * other field null too; the client then uploads what it has.
 */
public record SettingsResponse(ThemeMode mode, AccentColor accent, Language language, String lastTopicId,
                               Instant updatedAt) {

    public static SettingsResponse nothingSaved() {
        return new SettingsResponse(null, null, null, null, null);
    }
}
