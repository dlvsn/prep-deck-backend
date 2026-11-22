package denys.mazurenko.prepdeckbackend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** The eight interface accents. JSON carries the client's codes, "mint" to "red". */
public enum AccentColor {
    MINT("mint"),
    PINK("pink"),
    PURPLE("purple"),
    BLUE("blue"),
    ORANGE("orange"),
    GREEN("green"),
    YELLOW("yellow"),
    RED("red");

    private final String code;

    AccentColor(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static AccentColor fromCode(String code) {
        for (AccentColor accent : values()) {
            if (accent.code.equals(code)) {
                return accent;
            }
        }
        throw new IllegalArgumentException("Unknown accent: " + code);
    }
}
