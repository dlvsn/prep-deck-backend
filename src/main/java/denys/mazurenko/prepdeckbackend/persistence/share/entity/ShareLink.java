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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The public link of one tree. Opening it grants the topics it lists to whoever is signed in,
 * unless the owner took access from them before.
 */
@Entity
@Table(name = "share_links")
@Getter
@Setter
@NoArgsConstructor
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The root of the tree the link belongs to. */
    @Column(name = "topic_id", nullable = false, updatable = false)
    private UUID topicId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    /** Stored as it is: the owner may copy the link again at any time. */
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** All inside the tree, none inside another one listed (the service keeps it so). */
    @ElementCollection
    @CollectionTable(name = "share_link_topics", joinColumns = @JoinColumn(name = "link_id"))
    @Column(name = "topic_id", nullable = false)
    private Set<UUID> topicIds = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
