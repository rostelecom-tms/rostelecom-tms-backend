package ru.rt.rostelecom_tms.service.users;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.config.cache.CacheNames;
import ru.rt.rostelecom_tms.domain.projects.Project;
import ru.rt.rostelecom_tms.domain.users.RegistrationRequest;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.domain.users.UserRole;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserNotFoundException;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserRoleNotAllowedException;
import ru.rt.rostelecom_tms.dto.users.RegistrationRequestDto;
import ru.rt.rostelecom_tms.dto.users.RegistrationResponseDto;
import ru.rt.rostelecom_tms.repository.projects.ProjectRepository;
import ru.rt.rostelecom_tms.repository.users.RegistrationRequestRepository;
import ru.rt.rostelecom_tms.repository.users.UserRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserRoleService userRoleService;

    private final RegistrationRequestRepository registrationRequestRepository;

    private final ProjectRepository projectRepository;

    public record RegisterUserCommand(String email, String username, String password, String roleSlug, boolean canCreatePlans) {
    }

    public record UpdateUserCommand(Integer roleId, Boolean canCreatePlans) {
    }

    public List<User> findAll(User caller) {
        if (caller == null) {
            throw new UserRoleNotAllowedException("Authentication required");
        }

        String slug = caller.getRole().getSlug();
        if (RoleSlugs.ADMIN.equals(slug)) {
            return userRepository.findAll();
        }

        if (!RoleSlugs.TEAMLEAD.equals(slug) && !RoleSlugs.USER.equals(slug)) {
            throw new UserRoleNotAllowedException("Role is not allowed to view users");
        }

        List<Project> accessibleProjects = RoleSlugs.TEAMLEAD.equals(slug)
                ? projectRepository.findDistinctByOwnerIdOrMembersUserId(caller.getId(), caller.getId())
                : projectRepository.findDistinctByMembersUserId(caller.getId());

        Set<Integer> visibleUserIds = new LinkedHashSet<>();

        for (Project project : accessibleProjects) {
            if (project.getOwner() != null && project.getOwner().getId() != null) {
                visibleUserIds.add(project.getOwner().getId());
            }
            for (var member : project.getMembers()) {
                if (member.getUser() != null && member.getUser().getId() != null) {
                    visibleUserIds.add(member.getUser().getId());
                }
            }
        }

            List<User> overlapUsers = userRepository.findAllById(visibleUserIds)
                .stream()
                .filter(user -> {
                    String role = user.getRole().getSlug();
                    return RoleSlugs.ADMIN.equals(role) || RoleSlugs.TEAMLEAD.equals(role);
                })
                .toList();

            if (RoleSlugs.USER.equals(slug)) {
                return overlapUsers.stream()
                    .sorted(Comparator.comparing(User::getId))
                    .toList();
            }

            Set<Integer> teamleadIds = userRepository.findByRole_Slug(RoleSlugs.TEAMLEAD)
                .stream()
                .map(User::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            List<User> allTeamleads = userRepository.findAllById(teamleadIds);

            Set<Integer> resultIds = new LinkedHashSet<>();
            overlapUsers.stream().map(User::getId).forEach(resultIds::add);
            allTeamleads.stream().map(User::getId).forEach(resultIds::add);

            return userRepository.findAllById(resultIds)
                .stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
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
    public void delete(int id, User caller) {
        if (caller != null && caller.getId() != null && caller.getId() == id) {
            throw new UserRoleNotAllowedException("Self-deletion is not allowed");
        }

        User user = findOne(id);
        if (RoleSlugs.ADMIN.equals(user.getRole().getSlug())) {
            throw new UserRoleNotAllowedException("Deleting admin users is not allowed");
        }

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

    @Transactional
    public void createRegistrationRequest(RegistrationRequestDto dto) {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail(dto.email());
        request.setUsername(dto.username());
        request.setPasswordHash(passwordEncoder.encode(dto.password()));

        registrationRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponseDto> findAllRegistrationRequests() {
        return registrationRequestRepository.findAll().stream()
                .map(req -> new RegistrationResponseDto(req.getId(), req.getEmail(), req.getUsername()))
                .toList();
    }

    @Transactional
    public void approveRegistration(int id) {
        RegistrationRequest request = registrationRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена"));

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setUsername(request.getUsername());
        newUser.setPasswordHash(request.getPasswordHash());
        newUser.setCreatedAt(Instant.now());
        newUser.setRole(userRoleService.findOneBySlug(RoleSlugs.USER));

        userRepository.save(newUser);
        registrationRequestRepository.delete(request);
    }

    @Transactional
    public void rejectRegistration(int id) {
        if (!registrationRequestRepository.existsById(id)) {
            throw new EntityNotFoundException("Заявка не найдена");
        }
        registrationRequestRepository.deleteById(id);
    }
}
