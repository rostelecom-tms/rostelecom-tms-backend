package ru.rt.rostelecom_tms.domain.cases;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "case_steps")
public class CaseStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseField;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @Column(name = "title", length = Integer.MAX_VALUE)
    private String title;

    @Column(name = "action", nullable = false, length = Integer.MAX_VALUE)
    private String action;

    @Column(name = "expected_result", length = Integer.MAX_VALUE)
    private String expectedResult;
}