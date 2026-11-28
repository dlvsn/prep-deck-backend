package denys.mazurenko.prepdeckbackend.domain;

/** What a caller wants to do with a topic and the cards in it; {@link TopicRole#allows} decides. */
public enum TopicAction {
    /** Open the topic, its subtopics and cards, practise them, copy them. */
    VIEW,
    /** Change the topic or its cards: create, rename, move, delete. */
    EDIT,
    /** Decide who else may see the topic. */
    SHARE
}
