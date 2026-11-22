package denys.mazurenko.prepdeckbackend.persistence.settings.dao;

import denys.mazurenko.prepdeckbackend.persistence.settings.entity.UserSettings;
import denys.mazurenko.prepdeckbackend.persistence.settings.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingsDaoServiceImpl implements UserSettingsDaoService {

    private final UserSettingsRepository userSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSettings> find(UUID userId) {
        return userSettingsRepository.findById(userId);
    }

    @Override
    @Transactional
    public UserSettings save(UserSettings settings) {
        return userSettingsRepository.save(settings);
    }
}
