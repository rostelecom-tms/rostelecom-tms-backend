package ru.rt.rostelecom_tms.domain.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.rt.rostelecom_tms.domain.plans.Plan;
import ru.rt.rostelecom_tms.domain.projects.Project;
import ru.rt.rostelecom_tms.domain.projects.ProjectMember;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = Integer.MAX_VALUE)
    private String passwordHash;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ColumnDefault("false")
    @Column(name = "can_create_plans", nullable = false)
    private boolean canCreatePlans = false;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "role_id", nullable = false)
    private UserRole role;

    @OneToMany(mappedBy = "responsibleUser")
    private Set<Plan> plans = new LinkedHashSet<>();

    @OneToMany(mappedBy = "executedBy")
    private Set<Run> runs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "owner")
    private Set<Project> ownedProjects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<ProjectMember> projectMemberships = new LinkedHashSet<>();
}