package ru.rt.rostelecom_tms.init;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.config.AdminBootstrapProperties;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.User;
import ru.rt.rostelecom_tms.domain.users.UserRole;
import ru.rt.rostelecom_tms.repository.users.UserRepository;
import ru.rt.rostelecom_tms.repository.users.UserRoleRepository;

import java.time.Instant;
import java.util.UUID;

@Component
public class BootstrapData implements ApplicationRunner {

    private final UserRoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties props;

    public BootstrapData(UserRoleRepository roleRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         AdminBootstrapProperties props) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        ensureRole(RoleSlugs.USER, "user");
        ensureRole(RoleSlugs.ADMIN, "admin");
        ensureRole(RoleSlugs.TEAMLEAD, "teamlead");

        ensureAdminUser();
    }

    private void ensureRole(String slug, String name) {
        if (roleRepository.existsBySlug(slug)) {
            return;
        }

        UserRole role = new UserRole();
        role.setSlug(slug);
        role.setName(name);
        roleRepository.save(role);
    }

    private void ensureAdminUser() {
        String adminEmail = props.email();
        boolean exists = userRepository.existsByEmail(adminEmail);
        if (exists) {
            return;
        }

        String password = props.password();
        if (password == null || password.isBlank()) {
            password = UUID.randomUUID().toString();
            System.out.println("bootstrap admin password: " + password);
        }

        UserRole adminRole = roleRepository.findBySlug(RoleSlugs.ADMIN)
                .orElseThrow(() -> new IllegalStateException("admin role is missing"));

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(adminRole);
        admin.setCreatedAt(Instant.now());

        userRepository.save(admin);
    }
}
