package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import denys.mazurenko.prepdeckbackend.persistence.share.repository.ShareBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareBlockDaoServiceImpl implements ShareBlockDaoService {

    private final ShareBlockRepository blockRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(UUID ownerId, UUID userId) {
        return blockRepository.existsByOwnerIdAndUserId(ownerId, userId);
    }

    @Override
    @Transactional
    public void block(UUID ownerId, UUID userId) {
        blockRepository.insertIfAbsent(ownerId, userId);
    }

    @Override
    @Transactional
    public void unblock(UUID ownerId, UUID userId) {
        blockRepository.deleteBlock(ownerId, userId);
    }
}
