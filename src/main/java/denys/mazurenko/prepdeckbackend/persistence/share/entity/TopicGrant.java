package denys.mazurenko.prepdeckbackend.persistence.share.entity;

import denys.mazurenko.prepdeckbackend.domain.TopicRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Someone other than the owner may see a topic and its whole subtree. Rows are written with
 * {@code insert … on conflict do nothing} (see the repository), never through this entity, so
 * two acceptances racing for the same topic cannot fail on the unique key.
 */
@Entity
@Table(name = "topic_grants")
@Getter
@Setter
@NoArgsConstructor
public class TopicGrant {

    @Id
    private UUID id;

    @Column(name = "topic_id", nullable = false, updatable = false)
    private UUID topicId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "grantee_id", nullable = false, updatable = false)
    private UUID granteeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private TopicRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private GrantSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Null until the grantee opens the topic. */
    @Column(name = "seen_at")
    private Instant seenAt;
}
