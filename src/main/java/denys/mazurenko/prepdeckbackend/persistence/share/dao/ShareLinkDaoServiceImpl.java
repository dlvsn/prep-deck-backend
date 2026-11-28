package denys.mazurenko.prepdeckbackend.persistence.share.dao;

import denys.mazurenko.prepdeckbackend.persistence.share.entity.ShareLink;
import denys.mazurenko.prepdeckbackend.persistence.share.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareLinkDaoServiceImpl implements ShareLinkDaoService {

    private final ShareLinkRepository linkRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ShareLink> findByRoot(UUID rootTopicId) {
        return linkRepository.findByTopicId(rootTopicId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShareLink> findByToken(String token) {
        return linkRepository.findByToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareLink> findEnabledOfOwner(UUID ownerId) {
        return linkRepository.findByOwnerIdAndEnabledTrue(ownerId);
    }

    @Override
    @Transactional
    public ShareLink save(ShareLink link) {
        return linkRepository.save(link);
    }
}
