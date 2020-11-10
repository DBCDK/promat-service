package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.CascadeType;
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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@NamedQueries({
        @NamedQuery(
                name = Case.GET_CASE_WITH_FAUST_NAME,
                query = Case.GET_CASE_WITH_FAUST_QUERY
        )
})

/** Entity for table holding cases
 *
 *  Correct table name 'case' is a reserved word and would
 *  properbly cause some problems for jpa if used, thus the
 *  plural form used here.
 */
@Entity
@Table(name = "cases")
public class Case {

    public static final String GET_CASE_WITH_FAUST_NAME =
            "Case.get.with.faust";
    public static final String GET_CASE_WITH_FAUST_QUERY =
            "SELECT c " +
              "FROM Case c " +
             "WHERE (c.primaryFaust = :primaryFaust OR FUNCTION('jsonb_contains', c.relatedFausts, CAST(:relatedFaust AS JSONB))) " +
               "AND c.status NOT IN (dk.dbc.promat.service.persistence.CaseStatus.CLOSED, dk.dbc.promat.service.persistence.CaseStatus.DONE)";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private Integer id;

    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private String title;

    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private String details;

    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private String primaryFaust;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonStringArrayConverter.class)
    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private List<String> relatedFausts;

    @OneToOne
    @JsonView({CaseView.Case.class})
    private Reviewer reviewer;

    @OneToMany
    @JoinTable(
            name = "caseSubjects",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @JsonView({CaseView.Case.class})
    private List<Subject> subjects;

    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private LocalDate created;

    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private LocalDate deadline;

    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private LocalDate assigned;

    @Enumerated(EnumType.STRING)
    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @JsonView({CaseView.CaseSummary.class, CaseView.Case.class})
    private MaterialType materialType;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "caseTasks",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    @JsonView({CaseView.Case.class})
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
        return id.equals(aCase.id) &&
                title.equals(aCase.title) &&
                Objects.equals(details, aCase.details) &&
                primaryFaust.equals(aCase.primaryFaust) &&
                Objects.equals(relatedFausts, aCase.relatedFausts) &&
                Objects.equals(reviewer, aCase.reviewer) &&
                Objects.equals(subjects, aCase.subjects) &&
                Objects.equals(created, aCase.created) &&
                Objects.equals(deadline, aCase.deadline) &&
                Objects.equals(assigned, aCase.assigned) &&
                status == aCase.status &&
                materialType == aCase.materialType &&
                Objects.equals(tasks, aCase.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, details, primaryFaust, relatedFausts, reviewer, subjects, created, deadline, assigned, status, materialType, tasks);
    }

    @Override
    public String toString() {
        return "Case{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", details='" + details + '\'' +
                ", primaryFaust='" + primaryFaust + '\'' +
                ", relatedFausts=" + relatedFausts +
                ", reviewer=" + reviewer +
                ", subjects=" + subjects +
                ", created=" + created +
                ", deadline=" + deadline +
                ", assigned=" + assigned +
                ", status=" + status +
                ", materialType=" + materialType +
                ", tasks=" + tasks +
                '}';
    }
}
