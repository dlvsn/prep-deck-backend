package denys.mazurenko.prepdeckbackend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Dark or light interface. JSON carries the client's codes, "dark" and "light". */
public enum ThemeMode {
    DARK("dark"),
    LIGHT("light");

    private final String code;

    ThemeMode(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static ThemeMode fromCode(String code) {
        for (ThemeMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown theme mode: " + code);
    }
}
