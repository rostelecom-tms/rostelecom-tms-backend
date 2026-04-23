package ru.rt.rostelecom_tms.domain.rag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "logs_analysis_history")
public class LogsAnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "defect_id", nullable = false)
    private Integer defectId;

    @Column(name = "logs_text", nullable = false, length = Integer.MAX_VALUE)
    private String logsText;

    @Column(name = "answer_text", nullable = false, length = Integer.MAX_VALUE)
    private String answerText;

    @Column(name = "llm_provider", length = 40)
    private String llmProvider;

    @Column(name = "llm_model", length = 120)
    private String llmModel;

    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
