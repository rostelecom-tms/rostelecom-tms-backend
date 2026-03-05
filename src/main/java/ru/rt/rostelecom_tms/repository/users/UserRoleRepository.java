package ru.rt.rostelecom_tms.repository.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.users.UserRole;

import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
    Optional<UserRole> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
