package denys.mazurenko.prepdeckbackend.persistence.settings.dao;

import denys.mazurenko.prepdeckbackend.persistence.settings.entity.UserSettings;

import java.util.Optional;
import java.util.UUID;

public interface UserSettingsDaoService {

    Optional<UserSettings> find(UUID userId);

    UserSettings save(UserSettings settings);
}
