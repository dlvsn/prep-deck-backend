package denys.mazurenko.prepdeckbackend.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * The caller's role on a topic. Every topic answers {@link #OWNER} while topics cannot be shared;
 * the other two arrive with sharing and the client already hides actions by them.
 */
public enum TopicRole {
    OWNER,
    EDITOR,
    VIEWER;

    @JsonValue
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
