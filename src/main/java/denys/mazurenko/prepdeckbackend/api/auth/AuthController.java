package denys.mazurenko.prepdeckbackend.api.auth;

import denys.mazurenko.prepdeckbackend.api.ApiPaths;
import denys.mazurenko.prepdeckbackend.dto.auth.request.LoginRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RefreshRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RegisterRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.ResendCodeRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.VerifyEmailRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.response.RegistrationResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.SessionResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.dto.common.response.ApiResponse;
import denys.mazurenko.prepdeckbackend.service.auth.AuthWebService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthWebService authWebService;

    /**
     * Creates the account unconfirmed and emails a code; the session comes with the code.
     * The letter's language follows the request's Accept-Language.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<RegistrationResponse> register(@Valid
                                                      @RequestBody RegisterRequest request,
                                                      Locale locale) {
        return ApiResponse.ok(authWebService.register(request, locale));
    }

    /**
     * Confirms the email with the code from the letter and opens the first session.
     */
    @PostMapping("/verify-email")
    public ApiResponse<SessionResponse> verifyEmail(@Valid
                                                    @RequestBody VerifyEmailRequest request) {
        return ApiResponse.ok(authWebService.verifyEmail(request));
    }

    /**
     * Sends a new confirmation letter, at most once per cooldown.
     */
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid
                                                @RequestBody ResendCodeRequest request,
                                                Locale locale) {
        authWebService.resendVerification(request, locale);
        return ApiResponse.ok();
    }

    /**
     * Opens a session by email or username and password; an unconfirmed email gets a code instead.
     */
    @PostMapping("/login")
    public ApiResponse<SessionResponse> login(@Valid
                                              @RequestBody LoginRequest request,
                                              Locale locale) {
        return ApiResponse.ok(authWebService.login(request, locale));
    }

    /**
     * Exchanges a refresh token for a new access token and the next refresh token.
     */
    @PostMapping("/refresh")
    public ApiResponse<SessionResponse> refresh(@Valid
                                                @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authWebService.refresh(request));
    }

    /**
     * Revokes the session the refresh token belongs to; open, since the access token may have expired.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid
                                    @RequestBody RefreshRequest request) {
        authWebService.logout(request);
        return ApiResponse.ok();
    }

    /**
     * The signed-in user.
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(authWebService.me(UUID.fromString(jwt.getSubject())));
    }

    /**
     * Whether a username is still free, for the sign-up form's live hint.
     */
    @GetMapping("/username-available")
    public ApiResponse<Boolean> usernameAvailable(@RequestParam String username) {
        return ApiResponse.ok(authWebService.isUsernameFree(username));
    }
}
