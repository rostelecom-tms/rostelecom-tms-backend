package ru.rt.rostelecom_tms.service.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotCreatedException;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotFoundException;
import ru.rt.rostelecom_tms.repository.users.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserRoleService userRoleService;

    public record RegisterUserCommand(String email, String username, String password) {
    }

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRoleService = userRoleService;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findOne(int id) {
        Optional<User> foundUser = userRepository.findById(id);
        return foundUser.orElseThrow(UserNotFoundException::new);
    }

    @Transactional
    public void save(User user) {
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void update(int id, User updatedUser) {
        updatedUser.setId(id);
        userRepository.save(updatedUser);
    }

    @Transactional
    public void delete(int id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void register(RegisterUserCommand r) {
        if (userRepository.existsByEmail(r.email())) {
            throw new UserNotCreatedException("email already exists");
        }
        if (userRepository.existsByUsername(r.username())) {
            throw new UserNotCreatedException("username already exists");
        }

        User user = new User();
        user.setEmail(r.email());
        user.setUsername(r.username());
        user.setPasswordHash(passwordEncoder.encode(r.password())); // важно
        user.setRole(userRoleService.findOneBySlug("user"));
        this.save(user);
    }
}
