package denys.mazurenko.prepdeckbackend.persistence.user.dao;

import denys.mazurenko.prepdeckbackend.persistence.user.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDaoService {

    Optional<User> findById(UUID id);

    /** Ids that match nobody are left out. */
    List<User> findAllById(Collection<UUID> ids);

    /** By email when the login holds an "@", by username otherwise. Expects it lower-cased. */
    Optional<User> findByLogin(String login);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    User save(User user);
}
