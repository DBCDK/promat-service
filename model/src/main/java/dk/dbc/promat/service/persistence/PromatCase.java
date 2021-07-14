/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonView;
import dk.dbc.commons.jpa.converter.StringListToJsonArrayConverter;

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
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import javax.persistence.Transient;

@NamedQueries({
        @NamedQuery(
                name = PromatCase.GET_CASE_WITH_TASK_ID_NAME,
                query = PromatCase.GET_CASE_WITH_TASK_ID_QUERY),
        @NamedQuery(
                name = PromatCase.GET_CASES_FOR_PAYMENT_NAME,
                query = PromatCase.GET_CASES_FOR_PAYMENT_QUERY),
        @NamedQuery(
                name = PromatCase.GET_PAYED_CASES_NAME,
                query = PromatCase.GET_PAYED_CASES_QUERY),
        @NamedQuery(
                name = PromatCase.GET_CASES_FOR_UPDATE_NAME,
                query = PromatCase.GET_CASES_FOR_UPDATE_QUERY),
        @NamedQuery(
                name = PromatCase.GET_CASE_BY_FAUST_NAME,
                query = PromatCase.GET_CASE_BY_FAUST_QUERY),
        @NamedQuery(
                name = PromatCase.GET_CASES_FOR_REMINDERS_CHECK_NAME,
                query = PromatCase.GET_CASES_FOR_REMINDERS_CHECK_QUERY)
})
@Entity
public class PromatCase {
    public static final String TABLE_NAME = "promatcase";

    public static final String GET_CASE_WITH_TASK_ID_NAME =
            "PromatCase.get.case.with.task.id";
    public static final String GET_CASE_WITH_TASK_ID_QUERY = "select c" +
            "                                      from PromatCase c" +
            "                                      join c.tasks t" +
            "                                     where t.id=:taskid";

    public static final String GET_CASES_FOR_PAYMENT_NAME =
            "PromatCase.get.cases.for.payment";
    public static final String GET_CASES_FOR_PAYMENT_QUERY = "select c" +
            "                                                  from PromatCase c" +
            "                                                 where c.status in (dk.dbc.promat.service.persistence.CaseStatus.APPROVED," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.PENDING_MEETING," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.PENDING_EXPORT," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.EXPORTED," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.PENDING_REVERT," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.REVERTED," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.PENDING_CLOSE)" +
            "                                                 order by c.id";

    public static final String GET_PAYED_CASES_NAME =
            "PromatCase.get.payed.cases";
    public static final String GET_PAYED_CASES_QUERY = "select distinct c" +
            "                                             from PromatCase c" +
            "                                             join CaseTasks ct" +
            "                                               on c.id = ct.case_id" +
            "                                             join PromatTask t" +
            "                                               on t.id = ct.task_id" +
            "                                            where t.payed = :stamp" +
            "                                            order by c.id";

    public static final String GET_CASES_FOR_UPDATE_NAME =
            "PromatCase.get.cases.for.update";
    public static final String GET_CASES_FOR_UPDATE_QUERY = "select c" +
            "                                                  from PromatCase c" +
            "                                                 where c.status not in (dk.dbc.promat.service.persistence.CaseStatus.EXPORTED," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.PENDING_REVERT," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.REVERTED," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.PENDING_CLOSE," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.CLOSED," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.DELETED)" +
            "                                                 order by c.id";

    public static final String GET_CASE_BY_FAUST_NAME =
            "PromatCase.get.case.by.faust";
    public static final String GET_CASE_BY_FAUST_QUERY = "select distinct c" +
            "                                               from PromatCase c" +
            "                                               join CaseTasks ct" +
            "                                                 on c.id = ct.case_id" +
            "                                               join PromatTask t" +
            "                                                 on t.id = ct.task_id" +
            "                                              where (c.primaryFaust = :faust" +
            "                                                 or function('JsonbContainsFromString', t.targetFausts, :faust))" +
            "                                                and c.status not in (dk.dbc.promat.service.persistence.CaseStatus.PENDING_CLOSE," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.CLOSED," +
            "                                                                    dk.dbc.promat.service.persistence.CaseStatus.DELETED)";

    public static final String GET_CASES_FOR_REMINDERS_CHECK_NAME =
            "PromatCase.get.cases.for.reminders.check";
    public static final String GET_CASES_FOR_REMINDERS_CHECK_QUERY = "select c" +
            "                                                           from PromatCase c"+
            "                                                          where c.status in (dk.dbc.promat.service.persistence.CaseStatus.ASSIGNED," +
            "                                                                             dk.dbc.promat.service.persistence.CaseStatus.PENDING_ISSUES)";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private Integer id;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private String title;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private String details;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private String primaryFaust;

    // Todo: Replace by use of column targetFausts in promattask. Should be removed when
    //       we are certain that the frontend does not expect this field anywhere
    @Deprecated
    @Column(columnDefinition = "jsonb")
    @Convert(converter = StringListToJsonArrayConverter.class)
    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private List<String> relatedFausts;

    @OneToOne
    @JsonView({CaseView.Export.class, CaseView.Summary.class})
    private Reviewer reviewer;

    @OneToOne
    @JsonView({CaseView.Summary.class})
    private Editor editor;

    @OneToMany
    @JoinTable(
            name = "caseSubjects",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @JsonView({CaseView.Case.class})
    private List<Subject> subjects;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private LocalDate created;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private LocalDate deadline;

    @JsonView({CaseView.Summary.class, CaseView.Case.class})
    private LocalDate assigned;

    @Enumerated(EnumType.STRING)
    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private MaterialType materialType;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "caseTasks",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    @OrderBy(value = "id")
    @JsonView({CaseView.Export.class, CaseView.Case.class})
    private List<PromatTask> tasks;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private String weekCode;

    /*
     * weekcode without the catalog prefix, ie. yyyyww
     * Needed for 'less than' queries during export.
     */
    private String trimmedWeekCode;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private String author;

    @OneToOne
    @JsonView({CaseView.Summary.class, CaseView.Case.class})
    private Editor creator;

    @JsonView({CaseView.Export.class, CaseView.Summary.class, CaseView.Case.class})
    private String publisher;

    @JsonView({CaseView.Export.class, CaseView.Case.class})
    private String fulltextLink;

    @JsonView({CaseView.Case.class, CaseView.Summary.class})
    @Transient
    private Boolean newMessagesToEditor = false;

    @JsonView({CaseView.Case.class, CaseView.Summary.class})
    @Transient
    private Boolean newMessagesToReviewer = false;

    @JsonView({CaseView.Case.class, CaseView.Summary.class})
    @Transient
    private String note;

    @JsonView({CaseView.Case.class, CaseView.Summary.class})
    private LocalDate reminderSent;

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

    public Editor getEditor() {
        return editor;
    }

    public void setEditor(Editor editor) {
        this.editor = editor;
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

    public List<PromatTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<PromatTask> tasks) {
        this.tasks = tasks;
    }

    public String getWeekCode() {
        return weekCode;
    }

    public void setWeekCode(String weekCode) {
        this.weekCode = weekCode;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Editor getCreator() {
        return creator;
    }

    public void setCreator(Editor creator) {
        this.creator = creator;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public PromatCase withId(Integer id) {
        this.id = id;
        return this;
    }

    public PromatCase withTitle(String title) {
        this.title = title;
        return this;
    }

    public PromatCase withDetails(String details) {
        this.details = details;
        return this;
    }

    public PromatCase withPrimaryFaust(String primaryFaust) {
        this.primaryFaust = primaryFaust;
        return this;
    }

    public PromatCase withRelatedFausts(List<String> relatedFausts) {
        this.relatedFausts = relatedFausts;
        return this;
    }

    public PromatCase withReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public PromatCase withEditor(Editor editor) {
        this.editor = editor;
        return this;
    }

    public PromatCase withSubjects(List<Subject> subjects) {
        this.subjects = subjects;
        return this;
    }

    public PromatCase withCreated(LocalDate created) {
        this.created = created;
        return this;
    }

    public PromatCase withDeadline(LocalDate deadline) {
        this.deadline = deadline;
        return this;
    }

    public PromatCase withAssigned(LocalDate assigned) {
        this.assigned = assigned;
        return this;
    }

    public PromatCase withStatus(CaseStatus status) {
        this.status = status;
        return this;
    }

    public PromatCase withMaterialType(MaterialType materialType) {
        this.materialType = materialType;
        return this;
    }

    public PromatCase withTasks(List<PromatTask> tasks) {
        this.tasks = tasks;
        return this;
    }

    public PromatCase withWeekCode(String weekCode) {
        this.weekCode = weekCode;
        return this;
    }

    public PromatCase withAuthor(String author) {
        this.author = author;
        return this;
    }

    public PromatCase withCreator(Editor creator) {
        this.creator = creator;
        return this;
    }

    public PromatCase withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public String getTrimmedWeekCode() {
        return trimmedWeekCode;
    }

    public void setTrimmedWeekCode(String trimmedWeekCode) {
        this.trimmedWeekCode = trimmedWeekCode;
    }

    public PromatCase withTrimmedWeekCode(String trimmedWeekCode) {
        setTrimmedWeekCode(trimmedWeekCode);
        return this;
    }

    public String getFulltextLink() {
        return fulltextLink;
    }

    public void setFulltextLink(String fulltextLink) {
        this.fulltextLink = fulltextLink;
    }

    public PromatCase withFulltextLink(String fulltextLink) {
        setFulltextLink(fulltextLink);
        return this;
    }

    public Boolean getNewMessagesToEditor() {
        return newMessagesToEditor;
    }

    public PromatCase withNewMessagesToEditor(Boolean newMessagesToEditor) {
        this.newMessagesToEditor = newMessagesToEditor;
        return this;
    }

    public void setNewMessagesToEditor(Boolean newMessagesToEditor) {
        this.newMessagesToEditor = newMessagesToEditor;
    }

    public Boolean getNewMessagesToReviewer() {
        return newMessagesToReviewer;
    }

    public void setNewMessagesToReviewer(Boolean newMessagesToReviewer) {
        this.newMessagesToReviewer = newMessagesToReviewer;
    }

    public PromatCase withNewMessagesToReviewer(Boolean newMessagesToReviewer) {
        this.newMessagesToReviewer = newMessagesToReviewer;
        return this;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public PromatCase withNote(String note) {
        this.note = note;
        return this;
    }

    public LocalDate getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(LocalDate reminderSent) {
        this.reminderSent = reminderSent;
    }

    public PromatCase withReminder(LocalDate reminderSent) {
        this.reminderSent = reminderSent;
        return this;
    }

    @PrePersist
    @PreUpdate
    private void beforeUpdate() {
        if (weekCode != null) {
            trimmedWeekCode = weekCode.replaceAll("^\\D+", "");
        } else {
            trimmedWeekCode = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        PromatCase aCase = (PromatCase) o;
        return id.equals(aCase.id) &&
                title.equals(aCase.title) &&
                Objects.equals(details, aCase.details) &&
                primaryFaust.equals(aCase.primaryFaust) &&
                Objects.equals(relatedFausts, aCase.relatedFausts) &&
                Objects.equals(reviewer, aCase.reviewer) &&
                Objects.equals(editor, aCase.editor) &&
                Objects.equals(subjects, aCase.subjects) &&
                Objects.equals(created, aCase.created) &&
                Objects.equals(deadline, aCase.deadline) &&
                Objects.equals(assigned, aCase.assigned) &&
                status == aCase.status &&
                materialType == aCase.materialType &&
                Objects.equals(tasks, aCase.tasks) &&
                Objects.equals(weekCode, aCase.weekCode) &&
                Objects.equals(trimmedWeekCode, aCase.trimmedWeekCode) &&
                Objects.equals(author, aCase.author) &&
                Objects.equals(creator, aCase.creator) &&
                Objects.equals(publisher, aCase.publisher) &&
                Objects.equals(fulltextLink, aCase.fulltextLink) &&
                newMessagesToEditor == aCase.newMessagesToEditor &&
                newMessagesToReviewer == aCase.newMessagesToReviewer &&
                Objects.equals(reminderSent, aCase.reminderSent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, details, primaryFaust, relatedFausts, reviewer, editor, subjects, created,
                deadline, assigned, status, materialType, tasks, weekCode, trimmedWeekCode, author, creator, publisher,
                fulltextLink, newMessagesToEditor, newMessagesToReviewer, reminderSent);
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
                ", editor=" + editor +
                ", subjects=" + subjects +
                ", created=" + created +
                ", deadline=" + deadline +
                ", assigned=" + assigned +
                ", status=" + status +
                ", materialType=" + materialType +
                ", tasks=" + tasks +
                ", weekCode='" + weekCode + '\'' +
                ", trimmedWeekCode='" + trimmedWeekCode + '\'' +
                ", author='" + author + '\'' +
                ", creator=" + creator +
                ", publisher='" + publisher + '\'' +
                ", fulltextLink='" + fulltextLink + '\'' +
                ", newMessagesToEditor='" + newMessagesToEditor + '\'' +
                ", newMessagesToReviewer='" + newMessagesToReviewer + '\'' +
                ", note='" + note + '\'' +
                ", reminderSent'" + reminderSent + '\'' +
                '}';
    }
}
