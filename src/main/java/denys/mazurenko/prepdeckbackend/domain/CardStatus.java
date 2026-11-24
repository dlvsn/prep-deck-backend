package denys.mazurenko.prepdeckbackend.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Where a card stands for the caller, worked out from their practice. Practice is not built yet,
 * so every card is {@link #NEW} for now.
 */
public enum CardStatus {
    NEW,
    LEARNING,
    KNOWN;

    @JsonValue
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
