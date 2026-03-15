package ru.rt.rostelecom_tms.domain.plans;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.rt.rostelecom_tms.domain.cases.Case;
import ru.rt.rostelecom_tms.domain.runs.Run;
import ru.rt.rostelecom_tms.domain.users.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "introduction", length = Integer.MAX_VALUE)
    private String introduction;

    @Column(name = "approach", length = Integer.MAX_VALUE)
    private String approach;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "plan")
    private Set<Run> runs = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "plans_cases",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "case_id")
    )
    private List<Case> cases = new ArrayList<>();
}