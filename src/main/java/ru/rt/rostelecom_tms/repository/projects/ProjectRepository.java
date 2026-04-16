package ru.rt.rostelecom_tms.repository.projects;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.projects.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    boolean existsByName(String name);

    @EntityGraph(attributePaths = {"members", "members.user"})
    List<Project> findDistinctByOwnerIdOrMembersUserId(Integer ownerId, Integer memberUserId);

    @EntityGraph(attributePaths = {"members", "members.user"})
    Optional<Project> findOneById(Integer id);

    @EntityGraph(attributePaths = {"members", "members.user"})
    List<Project> findDistinctByMembersUserId(Integer userId);

    @EntityGraph(attributePaths = {"members", "members.user"})
    List<Project> findAllBy();
}
