package denys.mazurenko.prepdeckbackend.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * The caller's role on a topic, and the one list of what each role may do. Declared strongest
 * first: when several grants reach a topic, the earliest role wins.
 *
 * <p>{@link #EDITOR} is not granted yet (only the owner changes a topic); the client already
 * knows the code, and granting it later means allowing it in the database and in
 * {@link #isGrantable()}, not changing the services that ask {@link #allows}.
 */
public enum TopicRole {
    OWNER(EnumSet.allOf(TopicAction.class)),
    EDITOR(EnumSet.of(TopicAction.VIEW, TopicAction.EDIT)),
    VIEWER(EnumSet.of(TopicAction.VIEW));

    private final Set<TopicAction> actions;

    TopicRole(Set<TopicAction> actions) {
        this.actions = actions;
    }

    public boolean allows(TopicAction action) {
        return actions.contains(action);
    }

    /** Whether an owner can hand this role to someone else today. */
    public boolean isGrantable() {
        return this == VIEWER;
    }

    @JsonValue
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
