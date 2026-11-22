package denys.mazurenko.prepdeckbackend.dto.settings.request;

import denys.mazurenko.prepdeckbackend.domain.AccentColor;
import denys.mazurenko.prepdeckbackend.domain.Language;
import denys.mazurenko.prepdeckbackend.domain.ThemeMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Every pick at once: the client always sends the whole set it shows. */
public record UpdateSettingsRequest(
        @NotNull(message = "errors.badRequest")
        ThemeMode mode,

        @NotNull(message = "errors.badRequest")
        AccentColor accent,

        @NotNull(message = "errors.badRequest")
        Language language,

        /** Null when no topic is open. */
        @Size(max = 64, message = "errors.badRequest")
        String lastTopicId
) {
}
