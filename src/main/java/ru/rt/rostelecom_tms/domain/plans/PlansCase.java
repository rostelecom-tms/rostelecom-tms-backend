package ru.rt.rostelecom_tms.domain.plans;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.rt.rostelecom_tms.domain.cases.Case;

@Entity
@Table(name = "plans_cases")
public class PlansCase {
    @Id
    @ColumnDefault("nextval('plans_cases_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseField;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Case getCaseField() {
        return caseField;
    }

    public void setCaseField(Case caseField) {
        this.caseField = caseField;
    }

}