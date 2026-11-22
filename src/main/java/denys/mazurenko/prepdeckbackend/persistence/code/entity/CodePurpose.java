package denys.mazurenko.prepdeckbackend.persistence.code.entity;

/** What a one-time code unlocks. Each user has at most one active code per purpose. */
public enum CodePurpose {
    VERIFY_EMAIL,
    RESET_PASSWORD
}
