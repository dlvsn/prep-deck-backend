package denys.mazurenko.prepdeckbackend.api.settings;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import denys.mazurenko.prepdeckbackend.dto.settings.request.UpdateSettingsRequest;
import denys.mazurenko.prepdeckbackend.dto.settings.response.SettingsResponse;
import denys.mazurenko.prepdeckbackend.service.settings.SettingsWebService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.SETTINGS)
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsWebService settingsWebService;

    /**
     * The signed-in user's saved picks; all fields null before the first save.
     */
    @GetMapping
    public ApiResponse<SettingsResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(settingsWebService.get(UUID.fromString(jwt.getSubject())));
    }

    /**
     * Saves the whole set of picks.
     */
    @PutMapping
    public ApiResponse<SettingsResponse> update(@AuthenticationPrincipal Jwt jwt,
                                                @Valid
                                                @RequestBody UpdateSettingsRequest request) {
        return ApiResponse.ok(settingsWebService.update(UUID.fromString(jwt.getSubject()), request));
    }
}
