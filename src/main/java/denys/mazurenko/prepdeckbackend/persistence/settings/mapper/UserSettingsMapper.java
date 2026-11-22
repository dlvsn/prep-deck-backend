package denys.mazurenko.prepdeckbackend.persistence.settings.mapper;

import denys.mazurenko.prepdeckbackend.config.MapStructConfig;
import denys.mazurenko.prepdeckbackend.dto.settings.response.SettingsResponse;
import denys.mazurenko.prepdeckbackend.persistence.settings.entity.UserSettings;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface UserSettingsMapper {

    SettingsResponse toResponse(UserSettings settings);
}
