package ru.rt.rostelecom_tms.domain.cases;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "case_steps")
public class CaseStep {
    @Id
    @ColumnDefault("nextval('case_steps_id_seq')")
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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

}