package ru.rt.rostelecom_tms.domain.runs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.users.User;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "runs")
public class Run {
    @Id
    @ColumnDefault("nextval('runs_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseField;

    @ColumnDefault("now()")
    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "plan_id", nullable = false)
    private ru.rt.rostelecom_tms.domain.plans.Plan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "status_id", nullable = false)
    private RunStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "executed_by")
    private User executedBy;
}