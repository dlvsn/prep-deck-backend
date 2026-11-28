package denys.mazurenko.prepdeckbackend.persistence.share.entity;

/** How a grant came about. */
public enum GrantSource {
    /** A personal invitation was accepted, or the owner picked the person's topics. */
    INVITE,
    /** The person opened the tree's public link. */
    LINK
}
