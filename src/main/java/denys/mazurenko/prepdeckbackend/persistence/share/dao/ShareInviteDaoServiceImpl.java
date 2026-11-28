package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareInvite;
import denys.mazurenko.prepdeckbackend.persistence.share.repository.ShareInviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareInviteDaoServiceImpl implements ShareInviteDaoService {

    private final ShareInviteRepository inviteRepository;

    /** A lock that ended with its own transaction would protect nothing, so one must be open. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ShareInvite> findByHashForUpdate(String tokenHash) {
        return inviteRepository.findByHashForUpdate(tokenHash);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareInvite> findPendingOfOwner(UUID ownerId, Instant now) {
        return inviteRepository.findPendingOfOwner(ownerId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShareInvite> findLatest(UUID ownerId, String email) {
        return inviteRepository.findFirstByOwnerIdAndEmailOrderByCreatedAtDesc(ownerId, email);
    }

    @Override
    @Transactional
    public ShareInvite save(ShareInvite invite) {
        return inviteRepository.save(invite);
    }

    @Override
    @Transactional
    public List<ShareInvite> saveAll(List<ShareInvite> invites) {
        return inviteRepository.saveAll(invites);
    }
}
