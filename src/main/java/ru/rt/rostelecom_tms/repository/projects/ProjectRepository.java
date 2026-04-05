package ru.rt.rostelecom_tms.repository.projects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.projects.Project;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    boolean existsByName(String name);

    @Query("""
            SELECT DISTINCT p FROM Project p
            LEFT JOIN FETCH p.members m
            LEFT JOIN FETCH m.user
            WHERE p.owner.id = :userId OR m.user.id = :userId
            """)
    List<Project> findAllOwnedOrMember(@Param("userId") Integer userId);

    @Query("""
            SELECT DISTINCT p FROM Project p
            LEFT JOIN FETCH p.members m
            LEFT JOIN FETCH m.user
            WHERE p.id = :id
            """)
    Optional<Project> findByIdWithMembers(@Param("id") Integer id);

    @Query("""
            SELECT DISTINCT p FROM Project p
            LEFT JOIN FETCH p.members m
            LEFT JOIN FETCH m.user
            WHERE p.id IN (
                SELECT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId
            )
            """)
    List<Project> findAllByMemberUserId(@Param("userId") Integer userId);

    @Query("""
            SELECT DISTINCT p FROM Project p
            LEFT JOIN FETCH p.members m
            LEFT JOIN FETCH m.user
            """)
    List<Project> findAllWithMembers();
}
