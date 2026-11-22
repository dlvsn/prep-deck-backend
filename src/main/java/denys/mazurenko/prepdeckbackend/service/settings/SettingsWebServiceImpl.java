package denys.mazurenko.prepdeckbackend.service.settings;

import denys.mazurenko.prepdeckbackend.dto.settings.request.UpdateSettingsRequest;
import denys.mazurenko.prepdeckbackend.dto.settings.response.SettingsResponse;
import denys.mazurenko.prepdeckbackend.persistence.settings.dao.UserSettingsDaoService;
import denys.mazurenko.prepdeckbackend.persistence.settings.entity.UserSettings;
import denys.mazurenko.prepdeckbackend.persistence.settings.mapper.UserSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsWebServiceImpl implements SettingsWebService {

    private final UserSettingsDaoService settings;
    private final UserSettingsMapper settingsMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SettingsResponse get(UUID userId) {
        return settings.find(userId).map(settingsMapper::toResponse).orElseGet(SettingsResponse::nothingSaved);
    }

    /** The row is made on the first save; there is nothing to merge, the request is the whole set. */
    @Override
    @Transactional
    public SettingsResponse update(UUID userId, UpdateSettingsRequest request) {
        UserSettings row = settings.find(userId).orElseGet(() -> {
            UserSettings created = new UserSettings();
            created.setUserId(userId);
            return created;
        });
        row.setMode(request.mode());
        row.setAccent(request.accent());
        row.setLanguage(request.language());
        row.setLastTopicId(request.lastTopicId());
        row.setUpdatedAt(clock.instant());
        return settingsMapper.toResponse(settings.save(row));
    }
}
