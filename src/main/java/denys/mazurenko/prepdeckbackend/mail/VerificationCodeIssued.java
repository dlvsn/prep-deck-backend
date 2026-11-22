package denys.mazurenko.prepdeckbackend.mail;

import denys.mazurenko.prepdeckbackend.domain.Language;

/**
 * A code was saved and its letter should go out. Published inside the transaction that saved it
 * and handled only after that transaction commits.
 */
public record VerificationCodeIssued(String email,
                                     String username,
                                     String code,
                                     Language language,
                                     long ttlMinutes) {
}
