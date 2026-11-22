package denys.mazurenko.prepdeckbackend.persistence.code.repository;

import denys.mazurenko.prepdeckbackend.persistence.code.entity.CodePurpose;
import denys.mazurenko.prepdeckbackend.persistence.code.entity.OneTimeCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, UUID> {

    /** The code still waiting to be used, row-locked so two guesses at once count as two. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OneTimeCode> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId, CodePurpose purpose);

    /** The newest code, used or not: when it was sent decides whether another may go out yet. */
    Optional<OneTimeCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, CodePurpose purpose);

    @Modifying
    @Query("delete from OneTimeCode c where c.user.id = :userId and c.purpose = :purpose and c.usedAt is null")
    int deleteActive(@Param("userId") UUID userId, @Param("purpose") CodePurpose purpose);
}
