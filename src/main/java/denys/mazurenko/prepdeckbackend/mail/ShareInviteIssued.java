package denys.mazurenko.prepdeckbackend.mail;

import denys.mazurenko.prepdeckbackend.domain.Language;

import java.util.List;

/**
 * An invitation was saved and its letter should go out. Published inside the transaction that
 * saved it and handled only after that transaction commits, so a rolled-back invitation sends
 * nothing. Carries everything the letter shows, read while the transaction was open.
 */
public record ShareInviteIssued(String email,
                                Language language,
                                String inviterName,
                                String inviterEmail,
                                String inviteUrl,
                                List<InvitedTopic> topics,
                                long cardsTotal) {
}
