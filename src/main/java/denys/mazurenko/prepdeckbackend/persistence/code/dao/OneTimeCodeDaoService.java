package denys.mazurenko.prepdeckbackend.persistence.code.dao;

import denys.mazurenko.prepdeckbackend.persistence.code.entity.CodePurpose;
import denys.mazurenko.prepdeckbackend.persistence.code.entity.OneTimeCode;

import java.util.Optional;
import java.util.UUID;

public interface OneTimeCodeDaoService {

    /** Locks the row until the caller's transaction ends; call it inside one. */
    Optional<OneTimeCode> findActiveForUpdate(UUID userId, CodePurpose purpose);

    Optional<OneTimeCode> findLatest(UUID userId, CodePurpose purpose);

    /** Drops every unused code of that purpose, so only the newest letter's code works. */
    int deleteActive(UUID userId, CodePurpose purpose);

    OneTimeCode save(OneTimeCode code);
}
