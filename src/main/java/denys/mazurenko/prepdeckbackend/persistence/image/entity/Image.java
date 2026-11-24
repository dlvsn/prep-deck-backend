package denys.mazurenko.prepdeckbackend.persistence.image.entity;

import denys.mazurenko.prepdeckbackend.domain.ImageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * An uploaded picture; its bytes are an object named by the id in the image store.
 *
 * <p>The id is chosen before the row exists: the bytes go to the store first, under that id, and
 * the row follows. {@link Persistable} tells Spring Data the entity is new while it has not been
 * saved, so saving it is a plain insert rather than a select followed by one.
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
public class Image implements Persistable<UUID> {

    @Id
    private UUID id;

    /** Who uploaded it: only they can put it on a card. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 16, updatable = false)
    private ImageType contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "width", nullable = false, updatable = false)
    private int width;

    @Column(name = "height", nullable = false, updatable = false)
    private int height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean fresh = true;

    @Override
    public boolean isNew() {
        return fresh;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    @PostPersist
    @PostLoad
    void markStored() {
        fresh = false;
    }
}
