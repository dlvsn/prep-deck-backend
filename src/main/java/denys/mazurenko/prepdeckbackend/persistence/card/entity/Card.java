package denys.mazurenko.prepdeckbackend.persistence.card.entity;

import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A question with a Markdown answer, its tags and up to five pictures, in one topic. */
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "question", nullable = false, length = 500)
    private String question;

    /** Markdown. */
    @Column(name = "answer", nullable = false, columnDefinition = "text")
    private String answer;

    /** Lower-case, without "#", no duplicates, in the order they were added. */
    @ElementCollection
    @CollectionTable(name = "card_tags", joinColumns = @JoinColumn(name = "card_id"))
    @OrderColumn(name = "position")
    @Column(name = "tag", nullable = false, length = 40)
    private List<String> tags = new ArrayList<>();

    /** Display order; the first is the main picture. A duplicate shares its original's images. */
    @ManyToMany
    @JoinTable(name = "card_images",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "image_id"))
    @OrderColumn(name = "position")
    private List<Image> images = new ArrayList<>();

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
