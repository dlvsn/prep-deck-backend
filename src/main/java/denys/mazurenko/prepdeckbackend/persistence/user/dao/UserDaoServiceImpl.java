package denys.mazurenko.prepdeckbackend.persistence.user.dao;

import denys.mazurenko.prepdeckbackend.persistence.user.entity.User;
import denys.mazurenko.prepdeckbackend.persistence.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDaoServiceImpl implements UserDaoService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllById(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : userRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByLogin(String login) {
        return login.contains("@") ? userRepository.findByEmail(login) : userRepository.findByUsername(login);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
