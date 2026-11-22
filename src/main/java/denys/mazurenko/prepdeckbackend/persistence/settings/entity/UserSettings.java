package denys.mazurenko.prepdeckbackend.persistence.settings.entity;

import denys.mazurenko.prepdeckbackend.domain.AccentColor;
import denys.mazurenko.prepdeckbackend.domain.Language;
import denys.mazurenko.prepdeckbackend.domain.ThemeMode;
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

/** A user's picks, the same on every device: look, language, the topic opened last. */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserSettings {

    /** The user's id: one row per user. */
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_mode", nullable = false, length = 8)
    private ThemeMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "accent", nullable = false, length = 16)
    private AccentColor accent;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 8)
    private Language language;

    @Column(name = "last_topic_id", length = 64)
    private String lastTopicId;

    /** Set by the service on every save; the client compares it to know a row exists. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
