package ru.rt.rostelecom_tms.repository.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.users.RegistrationRequest;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Integer> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}