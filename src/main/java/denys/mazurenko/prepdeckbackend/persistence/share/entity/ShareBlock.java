package denys.mazurenko.prepdeckbackend.persistence.share.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * An owner took access from this user: the owner's public links no longer let them in. Written
 * and removed by the repository's own statements; the entity exists to read it.
 */
@Entity
@Table(name = "share_blocks")
@IdClass(ShareBlockKey.class)
@Getter
@Setter
@NoArgsConstructor
public class ShareBlock {

    @Id
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
