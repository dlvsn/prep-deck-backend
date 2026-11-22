package denys.mazurenko.prepdeckbackend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * The languages the app speaks: the interface language a user picks and the language letters are
 * written in. {@link #code} is what JSON carries and the template file's language suffix.
 */
public enum Language {
    EN("en"),
    UK("uk");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    /** From the code JSON carries ("en", "uk"). */
    @JsonCreator
    public static Language fromCode(String code) {
        for (Language language : values()) {
            if (language.code.equals(code)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unknown language: " + code);
    }

    /** The request's language when we write in it, English otherwise. */
    public static Language fromLocale(Locale locale) {
        if (locale != null) {
            for (Language language : values()) {
                if (language.code.equals(locale.getLanguage())) {
                    return language;
                }
            }
        }
        return EN;
    }
}
