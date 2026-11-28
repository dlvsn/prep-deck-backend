package denys.mazurenko.prepdeckbackend.persistence.share.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/** The key of {@link ShareBlock}: one row per owner and user. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShareBlockKey implements Serializable {

    private UUID ownerId;

    private UUID userId;
}
