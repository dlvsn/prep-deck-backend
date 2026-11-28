package denys.mazurenko.prepdeckbackend.persistence.share.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A personal invitation sent by email. Pending while it is neither accepted, revoked nor expired;
 * only an account with {@link #email} can accept it, and only once.
 */
@Entity
@Table(name = "share_invites")
@Getter
@Setter
@NoArgsConstructor
public class ShareInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The root of the tree the invitation is for. */
    @Column(name = "topic_id", nullable = false, updatable = false)
    private UUID topicId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    /** Lower-cased. */
    @Column(name = "email", nullable = false, length = 320, updatable = false)
    private String email;

    /** SHA-256 of the token in the letter, hex. */
    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** All inside the tree, none inside another one listed (the service keeps it so). */
    @ElementCollection
    @CollectionTable(name = "share_invite_topics", joinColumns = @JoinColumn(name = "invite_id"))
    @Column(name = "topic_id", nullable = false)
    private Set<UUID> topicIds = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isPending(Instant now) {
        return acceptedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }
}
