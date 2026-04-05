package ru.rt.rostelecom_tms.service.users;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.users.RoleSlugs;
import ru.rt.rostelecom_tms.domain.users.UserRole;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserRoleNotFoundException;
import ru.rt.rostelecom_tms.repository.users.UserRoleRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public record CreateRoleCommand(String name, String slug) {
    }

    public record UpdateRoleCommand(String name, String slug) {
    }

    public List<UserRole> findAll() {
        return userRoleRepository.findAll();
    }

    public UserRole findOne(int id) {
        Optional<UserRole> foundRole = userRoleRepository.findById(id);
        return foundRole.orElseThrow(() -> new UserRoleNotFoundException("Could not find role with id: " + id));
    }

    public UserRole findOneBySlug(String slug) {
        Optional<UserRole> foundRole = userRoleRepository.findBySlug(slug);
        return foundRole.orElseThrow(() -> new UserRoleNotFoundException("Could not find role with slug: " + slug));
    }

    @Transactional
    public void save(CreateRoleCommand c) {
        RoleSlugs.assertNotReserved(c.slug());
        UserRole role = new UserRole();
        role.setName(c.name());
        role.setSlug(c.slug());
        userRoleRepository.save(role);
    }

    @Transactional
    public void update(int id, UpdateRoleCommand c) {
        UserRole role = findOne(id);
        if (c.name() != null) {
            role.setName(c.name());
        }
        if (c.slug() != null) {
            RoleSlugs.assertNotReserved(c.slug());
            role.setSlug(c.slug());
        }

        userRoleRepository.save(role);
    }

    @Transactional
    public void delete(int id) {
        userRoleRepository.deleteById(id);
    }
}
