package dk.dbc.promat.service.persistence;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Entity for table holding cases
 *
 *  Correct table name 'case' is a reserved word and would
 *  properbly cause some problems for jpa if used, thus the
 *  plural form used here.
 */
@Entity
@Table(name = "cases")
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    private String details;

    private String primaryFaust;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonStringArrayConverter.class)
    private List<String> relatedFausts;

    @OneToOne
    private Reviewer reviewer;

    @OneToMany
    @JoinTable(
            name = "caseSubjects",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    private List<Subject> subjects;

    private LocalDate created;

    private LocalDate deadline;

    private LocalDate assigned;

    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    private MaterialType materialType;

    @OneToMany
    @JoinTable(
            name = "caseTasks",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    private List<Task> tasks;

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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPrimaryFaust() {
        return primaryFaust;
    }

    public void setPrimaryFaust(String primaryFaust) {
        this.primaryFaust = primaryFaust;
    }

    public List<String> getRelatedFausts() {
        return relatedFausts;
    }

    public void setRelatedFausts(List<String> relatedFausts) {
        this.relatedFausts = relatedFausts;
    }

    public Reviewer getReviewer() {
        return reviewer;
    }

    public void setReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDate getAssigned() {
        return assigned;
    }

    public void setAssigned(LocalDate assigned) {
        this.assigned = assigned;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Case withId(Integer id) {
        this.id = id;
        return this;
    }

    public Case withTitle(String title) {
        this.title = title;
        return this;
    }

    public Case withDetails(String details) {
        this.details = details;
        return this;
    }

    public Case withPrimaryFaust(String primaryFaust) {
        this.primaryFaust = primaryFaust;
        return this;
    }

    public Case withRelatedFausts(List<String> relatedFausts) {
        this.relatedFausts = relatedFausts;
        return this;
    }

    public Case withReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public Case withSubjects(List<Subject> subjects) {
        this.subjects = subjects;
        return this;
    }

    public Case withCreated(LocalDate created) {
        this.created = created;
        return this;
    }

    public Case withDeadline(LocalDate deadline) {
        this.deadline = deadline;
        return this;
    }

    public Case withAssigned(LocalDate assigned) {
        this.assigned = assigned;
        return this;
    }

    public Case withStatus(CaseStatus status) {
        this.status = status;
        return this;
    }

    public Case withMaterialType(MaterialType materialType) {
        this.materialType = materialType;
        return this;
    }

    public Case withTasks(List<Task> tasks) {
        this.tasks = tasks;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Case aCase = (Case) o;
        return id == aCase.id &&
                reviewer == aCase.reviewer &&
                title.equals(aCase.title) &&
                details.equals(aCase.details) &&
                primaryFaust.equals(aCase.primaryFaust) &&
                relatedFausts.equals(aCase.relatedFausts) &&
                subjects.equals(aCase.subjects) &&
                created.equals(aCase.created) &&
                Objects.equals(deadline, aCase.deadline) &&
                Objects.equals(assigned, aCase.assigned) &&
                status == aCase.status &&
                materialType == aCase.materialType &&
                tasks.equals(aCase.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, details, primaryFaust, relatedFausts, reviewer, subjects, created, deadline, assigned, status, materialType, tasks);
    }
}
