package ru.rt.rostelecom_tms.service.users;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.config.cache.CacheNames;
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
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECTS_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.PLANS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public void update(int id, UpdateUserCommand updatedUser) {
        User user = findOne(id);

        if (updatedUser.roleId() != null) {
            UserRole nextRole = userRoleService.findOne(updatedUser.roleId());
            if (RoleSlugs.TEAMLEAD.equals(user.getRole().getSlug())
                    && !RoleSlugs.TEAMLEAD.equals(nextRole.getSlug())
                    && userRepository.countByRole_Slug(RoleSlugs.TEAMLEAD) <= 1) {
                throw new UserRoleNotAllowedException("At least one teamlead must remain in the system");
            }
            user.setRole(nextRole);
        }
        if (updatedUser.canCreatePlans() != null) {
            user.setCanCreatePlans(updatedUser.canCreatePlans());
        }
        userRepository.save(user);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECTS_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.PLANS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
    public void delete(int id) {
        User user = findOne(id);
        if (RoleSlugs.TEAMLEAD.equals(user.getRole().getSlug())
                && userRepository.countByRole_Slug(RoleSlugs.TEAMLEAD) <= 1) {
            throw new UserRoleNotAllowedException("At least one teamlead must remain in the system");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PROJECTS_LIST, allEntries = true),
            @CacheEvict(value = CacheNames.PLANS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.CASES_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.RUNS_PAGE, allEntries = true),
            @CacheEvict(value = CacheNames.DASHBOARD, allEntries = true)
    })
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
