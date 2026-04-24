package ru.rt.rostelecom_tms.repository.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.users.User;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByRole_Slug(String slug);

    long countByRole_Slug(String slug);
}

