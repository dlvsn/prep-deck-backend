package denys.mazurenko.prepdeckbackend.persistence.code.dao;

import denys.mazurenko.prepdeckbackend.persistence.code.entity.CodePurpose;
import denys.mazurenko.prepdeckbackend.persistence.code.entity.OneTimeCode;
import denys.mazurenko.prepdeckbackend.persistence.code.repository.OneTimeCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OneTimeCodeDaoServiceImpl implements OneTimeCodeDaoService {

    private final OneTimeCodeRepository oneTimeCodeRepository;

    /** A lock that ended with its own transaction would protect nothing, so one must be open. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<OneTimeCode> findActiveForUpdate(UUID userId, CodePurpose purpose) {
        return oneTimeCodeRepository.findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(userId, purpose);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OneTimeCode> findLatest(UUID userId, CodePurpose purpose) {
        return oneTimeCodeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose);
    }

    @Override
    @Transactional
    public int deleteActive(UUID userId, CodePurpose purpose) {
        return oneTimeCodeRepository.deleteActive(userId, purpose);
    }

    @Override
    @Transactional
    public OneTimeCode save(OneTimeCode code) {
        return oneTimeCodeRepository.save(code);
    }
}
