package ru.rt.rostelecom_tms.domain.cases;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.rt.rostelecom_tms.domain.plans.PlansCase;
import ru.rt.rostelecom_tms.domain.runs.Run;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @OneToMany(mappedBy = "caseField")
    private Set<CaseStep> caseSteps = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caseField")
    private Set<Defect> defects = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caseField")
    private Set<PlansCase> plansCases = new LinkedHashSet<>();

    @OneToMany(mappedBy = "caseField")
    private Set<Run> runs = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public CaseGroup getGroup() {
        return group;
    }

    public void setGroup(CaseGroup group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions;
    }

    public String getPostconditions() {
        return postconditions;
    }

    public void setPostconditions(String postconditions) {
        this.postconditions = postconditions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<CaseStep> getCaseSteps() {
        return caseSteps;
    }

    public void setCaseSteps(Set<CaseStep> caseSteps) {
        this.caseSteps = caseSteps;
    }

    public Set<Defect> getDefects() {
        return defects;
    }

    public void setDefects(Set<Defect> defects) {
        this.defects = defects;
    }

    public Set<PlansCase> getPlansCases() {
        return plansCases;
    }

    public void setPlansCases(Set<PlansCase> plansCases) {
        this.plansCases = plansCases;
    }

    public Set<Run> getRuns() {
        return runs;
    }

    public void setRuns(Set<Run> runs) {
        this.runs = runs;
    }

}