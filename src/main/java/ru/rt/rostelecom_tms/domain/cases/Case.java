package ru.rt.rostelecom_tms.domain.cases;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.rt.rostelecom_tms.domain.plans.PlansCase;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cases")
public class Case {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "group_id", nullable = false)
    private CaseGroup group;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "preconditions", length = Integer.MAX_VALUE)
    private String preconditions;

    @Column(name = "postconditions", length = Integer.MAX_VALUE)
    private String postconditions;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "caseField", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CaseStep> caseSteps = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caseField")
    private Set<Defect> defects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caseField")
    private Set<PlansCase> plansCases = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caseField")
    private Set<Run> runs = new LinkedHashSet<>();
}