package ru.rt.rostelecom_tms.repository.projects;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rt.rostelecom_tms.domain.projects.ProjectAccessRequest;
import ru.rt.rostelecom_tms.domain.projects.ProjectAccessRequestDestination;
import ru.rt.rostelecom_tms.domain.projects.ProjectAccessRequestStatus;

import java.util.List;

@Repository
public interface ProjectAccessRequestRepository extends JpaRepository<ProjectAccessRequest, Integer> {

    @EntityGraph(attributePaths = {"project", "project.owner", "requesterUser", "approverUser", "processedByUser"})
    List<ProjectAccessRequest> findByDestinationAndStatusOrderByCreatedAtAsc(
            ProjectAccessRequestDestination destination,
            ProjectAccessRequestStatus status
    );

    @EntityGraph(attributePaths = {"project", "project.owner", "requesterUser", "approverUser", "processedByUser"})
    List<ProjectAccessRequest> findByApproverUserIdAndStatusOrderByCreatedAtAsc(
            Integer approverUserId,
            ProjectAccessRequestStatus status
    );

    boolean existsByProjectIdAndRequesterUserIdAndStatus(
            Integer projectId,
            Integer requesterUserId,
            ProjectAccessRequestStatus status
    );
}
