package ru.rt.rostelecom_tms.service.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.domain.users.UserRole;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotFoundException;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserRoleNotAllowedException;
import ru.rt.rostelecom_tms.repository.users.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserRoleService userRoleService;

    public record RegisterUserCommand(String email, String username, String password, String roleSlug, boolean canCreatePlans) {
    }

    public record UpdateUserCommand(Integer roleId, Boolean canCreatePlans) {
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findOne(int id) {
        Optional<User> foundUser = userRepository.findById(id);
        return foundUser.orElseThrow(() -> new UserNotFoundException("Could not find user with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("Could not find user with email: " + email));
    }

    @Transactional
    public void update(int id, UpdateUserCommand updatedUser) {
        User user = findOne(id);

        String currentSlug = user.getRole().getSlug();
        if (updatedUser.roleId() != null) {
            UserRole nextRole = userRoleService.findOne(updatedUser.roleId());
            String nextSlug = nextRole.getSlug();
            if (RoleSlugs.TEAMLEAD.equals(currentSlug)
                    && !RoleSlugs.TEAMLEAD.equals(nextSlug)
                    && userRepository.countByRole_Slug(RoleSlugs.TEAMLEAD) <= 1) {
                throw new UserRoleNotAllowedException("At least one teamlead must remain in the system");
            }
        }

        if (updatedUser.roleId() != null) {
            UserRole role = userRoleService.findOne(updatedUser.roleId());
            user.setRole(role);
        }
        if (updatedUser.canCreatePlans() != null) {
            user.setCanCreatePlans(updatedUser.canCreatePlans());
        }
        userRepository.save(user);
    }

    @Transactional
    public void delete(int id) {
        User user = findOne(id);
        if (RoleSlugs.TEAMLEAD.equals(user.getRole().getSlug())
                && userRepository.countByRole_Slug(RoleSlugs.TEAMLEAD) <= 1) {
            throw new UserRoleNotAllowedException("At least one teamlead must remain in the system");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void register(RegisterUserCommand r) {
        String slug = r.roleSlug() != null ? r.roleSlug() : RoleSlugs.USER;
        if (!slug.equals(RoleSlugs.USER) && !slug.equals(RoleSlugs.TEAMLEAD)) {
            throw new UserRoleNotAllowedException(
                    "Role '" + slug + "' cannot be assigned during registration"
            );
        }
        User user = new User();
        user.setEmail(r.email());
        user.setUsername(r.username());
        user.setPasswordHash(passwordEncoder.encode(r.password()));
        user.setRole(userRoleService.findOneBySlug(slug));
        user.setCanCreatePlans(r.canCreatePlans());
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
    }
}
