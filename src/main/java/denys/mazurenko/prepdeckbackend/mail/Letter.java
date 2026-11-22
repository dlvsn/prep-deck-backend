package denys.mazurenko.prepdeckbackend.mail;

/** A rendered letter: the subject line and the HTML body. */
public record Letter(String subject, String html) {
}
