package ru.rt.rostelecom_tms.repository.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.projects.ProjectMember;

import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {

    boolean existsByProjectIdAndUserId(Integer projectId, Integer userId);

    Optional<ProjectMember> findByProjectIdAndUserId(Integer projectId, Integer userId);
}
