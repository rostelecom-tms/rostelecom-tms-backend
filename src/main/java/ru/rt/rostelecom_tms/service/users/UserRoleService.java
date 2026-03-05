package ru.rt.rostelecom_tms.service.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rt.rostelecom_tms.domain.users.UserRole;
import ru.rt.rostelecom_tms.domain.users.exceptions.UserRoleNotFoundException;
import ru.rt.rostelecom_tms.repository.users.UserRoleRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    @Autowired
    public UserRoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    public List<UserRole> findAll() {
        return userRoleRepository.findAll();
    }

    public UserRole findOne(int id) {
        Optional<UserRole> foundRole = userRoleRepository.findById(id);
        return foundRole.orElseThrow(UserRoleNotFoundException::new);
    }

    public UserRole findOneBySlug(String slug) {
        Optional<UserRole> foundRole = userRoleRepository.findBySlug(slug);
        return foundRole.orElseThrow(UserRoleNotFoundException::new);
    }

    @Transactional
    public void save(UserRole role) {
        userRoleRepository.save(role);
    }

    @Transactional
    public void update(int id, UserRole updatedRole) {
        this.save(updatedRole);
    }

    @Transactional
    public void delete(int id) {
        userRoleRepository.deleteById(id);
    }
}
