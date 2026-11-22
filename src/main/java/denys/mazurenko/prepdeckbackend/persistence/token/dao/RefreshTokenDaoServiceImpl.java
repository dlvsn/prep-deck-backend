package denys.mazurenko.prepdeckbackend.persistence.token.dao;

import denys.mazurenko.prepdeckbackend.persistence.token.entity.RefreshToken;
import denys.mazurenko.prepdeckbackend.persistence.token.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenDaoServiceImpl implements RefreshTokenDaoService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** A lock that ended with its own transaction would protect nothing, so one must be open. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return refreshTokenRepository.findByHashForUpdate(tokenHash);
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public int revokeFamily(UUID familyId, Instant now) {
        return refreshTokenRepository.revokeFamily(familyId, now);
    }
}
