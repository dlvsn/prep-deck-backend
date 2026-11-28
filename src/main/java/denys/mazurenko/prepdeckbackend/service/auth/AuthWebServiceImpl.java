package denys.mazurenko.prepdeckbackend.service.auth;

import denys.mazurenko.prepdeckbackend.config.JwtProperties;
import denys.mazurenko.prepdeckbackend.config.VerificationProperties;
import denys.mazurenko.prepdeckbackend.domain.Language;
import denys.mazurenko.prepdeckbackend.dto.auth.request.LoginRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RefreshRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RegisterRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.ResendCodeRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.VerifyEmailRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.response.CodeAttemptsResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.RegistrationResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.SessionResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UnconfirmedEmailResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.dto.common.response.RetryAfterResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.mail.VerificationCodeIssued;
import denys.mazurenko.prepdeckbackend.persistence.code.dao.OneTimeCodeDaoService;
import denys.mazurenko.prepdeckbackend.persistence.code.entity.CodePurpose;
import denys.mazurenko.prepdeckbackend.persistence.code.entity.OneTimeCode;
import denys.mazurenko.prepdeckbackend.persistence.token.dao.RefreshTokenDaoService;
import denys.mazurenko.prepdeckbackend.persistence.token.entity.RefreshToken;
import denys.mazurenko.prepdeckbackend.persistence.user.dao.UserDaoService;
import denys.mazurenko.prepdeckbackend.persistence.user.entity.User;
import denys.mazurenko.prepdeckbackend.persistence.user.mapper.UserMapper;
import denys.mazurenko.prepdeckbackend.security.JwtService;
import denys.mazurenko.prepdeckbackend.security.OneTimeCodes;
import denys.mazurenko.prepdeckbackend.security.OpaqueTokens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Each method is one transaction: rotation reads a locked row, revokes it and inserts its
 * successor, and those must succeed or fail together. Letters go out only after the transaction
 * that saved their code commits ({@link VerificationCodeIssued}).
 */
@Slf4j
@Service
public class AuthWebServiceImpl implements AuthWebService {

    private final UserDaoService users;
    private final RefreshTokenDaoService refreshTokens;
    private final OneTimeCodeDaoService codes;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;
    private final VerificationProperties verification;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Compared against when the login matches nobody, so an unknown account costs the same BCrypt
     * time as a wrong password and response times do not reveal which accounts exist.
     */
    private final String dummyHash;

    public AuthWebServiceImpl(UserDaoService users, RefreshTokenDaoService refreshTokens, OneTimeCodeDaoService codes,
                              PasswordEncoder passwordEncoder, JwtService jwtService, UserMapper userMapper,
                              JwtProperties jwtProperties, VerificationProperties verification,
                              ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.codes = codes;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
        this.verification = verification;
        this.events = events;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // ---------------------------------------------------------------- sign-up and confirmation

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request, Locale locale) {
        String email = normalize(request.email());
        String username = normalize(request.username());

        // An account that never confirmed its email is nobody's yet: whoever registers the address
        // again takes it over, and only the one who can read the mailbox will get in.
        User user = users.findByLogin(email).orElse(null);
        if (user != null && user.getEmailVerifiedAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "auth.emailTaken");
        }
        boolean usernameIsOurs = user != null && user.getUsername().equals(username);
        if (!usernameIsOurs && users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "auth.usernameTaken");
        }

        if (user == null) {
            user = new User();
            user.setEmail(email);
        }
        user.setUsername(username);
        user.setDisplayName(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = users.save(user);

        sendCodeUnlessTooSoon(user, locale);
        return new RegistrationResponse(email, verification.getCodeTtl().toMinutes(), cooldownLeft(user).toSeconds());
    }

    /** Not rolled back on refusal: a wrong guess must use up an attempt even though we answer 400. */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public SessionResponse verifyEmail(VerifyEmailRequest request) {
        User user = users.findByLogin(normalize(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "auth.codeExpired"));
        if (user.getEmailVerifiedAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "auth.emailAlreadyVerified");
        }
        OneTimeCode code = codes.findActiveForUpdate(user.getId(), CodePurpose.VERIFY_EMAIL)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "auth.codeExpired"));
        Instant now = clock.instant();
        if (!code.getExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "auth.codeExpired");
        }
        if (!OneTimeCodes.matches(user.getId(), request.code(), code.getCodeHash())) {
            code.setAttemptsLeft(code.getAttemptsLeft() - 1);
            if (code.getAttemptsLeft() <= 0) {
                code.setUsedAt(now);
                codes.save(code);
                throw new ApiException(HttpStatus.BAD_REQUEST, "auth.codeAttemptsExhausted");
            }
            codes.save(code);
            throw new ApiException(HttpStatus.BAD_REQUEST, "auth.codeInvalid",
                    new CodeAttemptsResponse(code.getAttemptsLeft()));
        }

        code.setUsedAt(now);
        codes.save(code);
        user.setEmailVerifiedAt(now);
        users.save(user);
        return startSession(user, UUID.randomUUID());
    }

    /** Quiet about addresses it does not know or already confirmed, so it reveals no accounts. */
    @Override
    @Transactional
    public void resendVerification(ResendCodeRequest request, Locale locale) {
        Optional<User> found = users.findByLogin(normalize(request.email()));
        if (found.isEmpty() || found.get().getEmailVerifiedAt() != null) {
            return;
        }
        User user = found.get();
        Duration wait = cooldownLeft(user);
        if (!wait.isZero()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "auth.resendTooSoon",
                    new RetryAfterResponse(wait.toSeconds()));
        }
        sendCodeUnlessTooSoon(user, locale);
    }

    // ---------------------------------------------------------------- sessions

    /** Not rolled back on refusal: the code sent to an unconfirmed account must stay valid. */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public SessionResponse login(LoginRequest request, Locale locale) {
        Optional<User> found = users.findByLogin(normalize(request.login()));
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "auth.invalidCredentials");
        }
        User user = found.get();
        if (user.getEmailVerifiedAt() == null) {
            sendCodeUnlessTooSoon(user, locale);
            throw new ApiException(HttpStatus.FORBIDDEN, "auth.emailNotVerified",
                    new UnconfirmedEmailResponse(user.getEmail(), cooldownLeft(user).toSeconds()));
        }
        // A hash made with weaker settings than today's is replaced while the password is at hand.
        if (passwordEncoder.upgradeEncoding(hash)) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            users.save(user);
        }
        return startSession(user, UUID.randomUUID());
    }

    /** Not rolled back on refusal: revoking a family after a reused token must stick. */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public SessionResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokens.findByHashForUpdate(OpaqueTokens.hash(request.refreshToken()))
                .orElseThrow(ApiException::sessionExpired);
        Instant now = clock.instant();
        if (token.getRevokedAt() != null) {
            int revoked = refreshTokens.revokeFamily(token.getFamilyId(), now);
            log.warn("Revoked refresh token reused for user {}; revoked {} more of its family", token.getUser().getId(), revoked);
            throw ApiException.sessionExpired();
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw ApiException.sessionExpired();
        }
        token.setRevokedAt(now);
        refreshTokens.save(token);
        return startSession(token.getUser(), token.getFamilyId());
    }

    /** Revokes the token's whole family: the session of this one device. */
    @Override
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokens.findByHashForUpdate(OpaqueTokens.hash(request.refreshToken()))
                .ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return users.findById(userId).map(userMapper::toResponse).orElseThrow(ApiException::sessionExpired);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameFree(String username) {
        return !users.existsByUsername(normalize(username));
    }

    // ---------------------------------------------------------------- helpers

    private SessionResponse startSession(User user, UUID familyId) {
        String raw = OpaqueTokens.generate();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(OpaqueTokens.hash(raw));
        token.setFamilyId(familyId);
        token.setExpiresAt(clock.instant().plus(jwtProperties.getRefreshTtl()));
        refreshTokens.save(token);
        return new SessionResponse(jwtService.issueAccessToken(user), raw, userMapper.toResponse(user));
    }

    /**
     * Replaces any unused code with a new one and queues its letter, unless the last letter went
     * out less than the cooldown ago: then that one still stands, and nobody gets flooded.
     */
    private void sendCodeUnlessTooSoon(User user, Locale locale) {
        if (!cooldownLeft(user).isZero()) {
            return;
        }
        codes.deleteActive(user.getId(), CodePurpose.VERIFY_EMAIL);
        String raw = OneTimeCodes.generate();
        OneTimeCode code = new OneTimeCode();
        code.setUser(user);
        code.setPurpose(CodePurpose.VERIFY_EMAIL);
        code.setCodeHash(OneTimeCodes.hash(user.getId(), raw));
        code.setAttemptsLeft(verification.getMaxAttempts());
        code.setExpiresAt(clock.instant().plus(verification.getCodeTtl()));
        codes.save(code);
        events.publishEvent(new VerificationCodeIssued(user.getEmail(), user.getDisplayName(), raw,
                Language.fromLocale(locale), verification.getCodeTtl().toMinutes()));
    }

    /** Time until another letter may go to this user; zero when one may go now. */
    private Duration cooldownLeft(User user) {
        return codes.findLatest(user.getId(), CodePurpose.VERIFY_EMAIL)
                .map(last -> Duration.between(clock.instant(), last.getCreatedAt().plus(verification.getResendCooldown())))
                .filter(left -> left.isPositive())
                .orElse(Duration.ZERO);
    }


    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
