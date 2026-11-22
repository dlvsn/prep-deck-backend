package denys.mazurenko.prepdeckbackend.service.auth;

import denys.mazurenko.prepdeckbackend.config.JwtProperties;
import denys.mazurenko.prepdeckbackend.dto.auth.request.LoginRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RefreshRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.request.RegisterRequest;
import denys.mazurenko.prepdeckbackend.dto.auth.response.SessionResponse;
import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;
import denys.mazurenko.prepdeckbackend.exception.ApiException;
import denys.mazurenko.prepdeckbackend.persistence.token.dao.RefreshTokenDaoService;
import denys.mazurenko.prepdeckbackend.persistence.token.entity.RefreshToken;
import denys.mazurenko.prepdeckbackend.persistence.user.dao.UserDaoService;
import denys.mazurenko.prepdeckbackend.persistence.user.entity.User;
import denys.mazurenko.prepdeckbackend.persistence.user.mapper.UserMapper;
import denys.mazurenko.prepdeckbackend.security.JwtService;
import denys.mazurenko.prepdeckbackend.security.OpaqueTokens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Each method is one transaction: rotation reads a locked row, revokes it and inserts its
 * successor, and those must succeed or fail together.
 */
@Slf4j
@Service
public class AuthWebServiceImpl implements AuthWebService {

    private final UserDaoService users;
    private final RefreshTokenDaoService refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    /**
     * Compared against when the login matches nobody, so an unknown account costs the same BCrypt
     * time as a wrong password and response times do not reveal which accounts exist.
     */
    private final String dummyHash;

    public AuthWebServiceImpl(UserDaoService users, RefreshTokenDaoService refreshTokens, PasswordEncoder passwordEncoder,
                              JwtService jwtService, UserMapper userMapper, JwtProperties jwtProperties, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public SessionResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        String username = normalize(request.username());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "auth.emailTaken");
        }
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "auth.usernameTaken");
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setDisplayName(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return startSession(users.save(user), UUID.randomUUID());
    }

    @Override
    @Transactional
    public SessionResponse login(LoginRequest request) {
        Optional<User> found = users.findByLogin(normalize(request.login()));
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "auth.invalidCredentials");
        }
        User user = found.get();
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
