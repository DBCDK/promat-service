/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseRequest implements Dto {

    public CaseRequest() {}

    public CaseRequest(PromatCase promatCase) {
        this.author = promatCase.getAuthor();
        if (promatCase.getCreator() != null) {
            this.creator = promatCase.getCreator().getId();
        }
        this.deadline = promatCase.getDeadline().toString();
        this.details = promatCase.getDetails();
        if (promatCase.getEditor() != null) {
            this.editor = promatCase.getEditor().getId();
        }
        this.materialType = promatCase.getMaterialType();
        this.primaryFaust = promatCase.getPrimaryFaust();
        this.publisher = promatCase.getPublisher();
        this.recordId = promatCase.getRecordId();
        this.relatedFausts = promatCase.getRelatedFausts();
        if (promatCase.getReviewer() != null) {
            this.reviewer = promatCase.getReviewer().getId();
        }
        this.status = promatCase.getStatus();
        if (promatCase.getSubjects() != null) {
            this.subjects = promatCase.getSubjects().stream().map(subject -> subject.getId()).collect(Collectors.toList());
        }
        this.title = promatCase.getTitle();
        this.weekCode = promatCase.getWeekCode();
        this.fulltextLink = promatCase.getFulltextLink();
    }

    private String title;

    private String details;

    private String primaryFaust;

    private List<String> relatedFausts;

    private Integer reviewer = null;

    private Integer editor = null;

    private List<Integer> subjects;

    private String deadline;

    private String assigned;

    private CaseStatus status;

    private MaterialType materialType;

    private List<TaskDto> tasks;

    private String weekCode;

    private String author;

    private Integer creator;

    private String publisher;

    private String recordId;

    private String note;

    private String fulltextLink;

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

    public Integer getReviewer() {
        return reviewer;
    }

    public void setReviewer(Integer reviewer) {
        this.reviewer = reviewer;
    }

    public Integer getEditor() {
        return editor;
    }

    public void setEditor(Integer reviewer) {
        this.editor = reviewer;
    }

    public List<Integer> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Integer> subjects) {
        this.subjects = subjects;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getAssigned() {
        return assigned;
    }

    public void setAssigned(String assigned) {
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

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
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

    public Integer getCreator() {
        return creator;
    }

    public void setCreator(Integer creator) {
        this.creator = creator;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public CaseRequest withTitle(String title) {
        this.title = title;
        return this;
    }

    public CaseRequest withDetails(String details) {
        this.details = details;
        return this;
    }

    public CaseRequest withPrimaryFaust(String primaryFaust) {
        this.primaryFaust = primaryFaust;
        return this;
    }

    public CaseRequest withRelatedFausts(List<String> relatedFausts)
    {
        this.relatedFausts = relatedFausts;
        return this;
    }

    public CaseRequest withReviewer(Integer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public CaseRequest withEditor(Integer editor) {
        this.editor = editor;
        return this;
    }

    public CaseRequest withSubjects(List<Integer> subjects) {
        this.subjects = subjects;
        return this;
    }

    public CaseRequest withDeadline(String deadline) {
        this.deadline = deadline;
        return this;
    }

    public CaseRequest withAssigned(String assigned) {
        this.assigned = assigned;
        return this;
    }

    public CaseRequest withStatus(CaseStatus status) {
        this.status = status;
        return this;
    }

    public CaseRequest withMaterialType(MaterialType materialType) {
        this.materialType = materialType;
        return this;
    }

    public CaseRequest withTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
        return this;
    }

    public CaseRequest withWeekCode(String weekCode) {
        this.weekCode = weekCode;
        return this;
    }

    public CaseRequest withAuthor(String author) {
        this.author = author;
        return this;
    }

    public CaseRequest withCreator(Integer creator) {
        this.creator = creator;
        return this;
    }

    public CaseRequest withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public CaseRequest withNote(String note) {
        this.note = note;
        return this;
    }
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public CaseRequest withRecordId(String recordId) {
        setRecordId(recordId);
        return this;
    }

    public String getFulltextLink() {
        return fulltextLink;
    }

    public void setFulltextLink(String fulltextLink) {
        this.fulltextLink = fulltextLink;
    }

    public CaseRequest withFulltextLink(String fulltextLink) {
        setFulltextLink(fulltextLink);
        return this;
    }

    @Override
    public String toString() {
        return "CaseRequestDto{" +
                "title='" + title + '\'' +
                ", details='" + details + '\'' +
                ", primaryFaust='" + primaryFaust + '\'' +
                ", relatedFausts=" + relatedFausts +
                ", reviewer=" + reviewer +
                ", editor=" + editor +
                ", subjects=" + subjects +
                ", deadline='" + deadline + '\'' +
                ", assigned='" + assigned + '\'' +
                ", status=" + status +
                ", materialType=" + materialType +
                ", tasks=" + tasks +
                ", weekCode='" + weekCode + '\'' +
                ", author='" + author + '\'' +
                ", creator=" + creator +
                ", publisher='" + publisher + '\'' +
                ", recordId='" + recordId + '\'' +
                ", note='" + note + '\'' +
                ", fulltextLink='" + fulltextLink + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CaseRequest that = (CaseRequest) o;

        if (title != null ? !title.equals(that.title) : that.title != null) {
            return false;
        }
        if (details != null ? !details.equals(that.details) : that.details != null) {
            return false;
        }
        if (primaryFaust != null ? !primaryFaust.equals(that.primaryFaust) : that.primaryFaust != null) {
            return false;
        }
        if (relatedFausts != null ? !relatedFausts.equals(that.relatedFausts) : that.relatedFausts != null) {
            return false;
        }
        if (reviewer != null ? !reviewer.equals(that.reviewer) : that.reviewer != null) {
            return false;
        }
        if (editor != null ? !editor.equals(that.editor) : that.editor != null) {
            return false;
        }
        if (subjects != null ? !subjects.equals(that.subjects) : that.subjects != null) {
            return false;
        }
        if (deadline != null ? !deadline.equals(that.deadline) : that.deadline != null) {
            return false;
        }
        if (assigned != null ? !assigned.equals(that.assigned) : that.assigned != null) {
            return false;
        }
        if (status != that.status) {
            return false;
        }
        if (materialType != that.materialType) {
            return false;
        }
        if (tasks != null ? !tasks.equals(that.tasks) : that.tasks != null) {
            return false;
        }
        if (weekCode != null ? !weekCode.equals(that.weekCode) : that.weekCode != null) {
            return false;
        }
        if (author != null ? !author.equals(that.author) : that.author != null) {
            return false;
        }
        if (creator != null ? !creator.equals(that.creator) : that.creator != null) {
            return false;
        }
        if (publisher != null ? !publisher.equals(that.publisher) : that.publisher != null) {
            return false;
        }
        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) {
            return false;
        }
        if (note != null ? !note.equals(that.note) : that.note != null) {
            return false;
        }
        return fulltextLink != null ? fulltextLink.equals(that.fulltextLink) : that.fulltextLink == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (details != null ? details.hashCode() : 0);
        result = 31 * result + (primaryFaust != null ? primaryFaust.hashCode() : 0);
        result = 31 * result + (relatedFausts != null ? relatedFausts.hashCode() : 0);
        result = 31 * result + (reviewer != null ? reviewer.hashCode() : 0);
        result = 31 * result + (editor != null ? editor.hashCode() : 0);
        result = 31 * result + (subjects != null ? subjects.hashCode() : 0);
        result = 31 * result + (deadline != null ? deadline.hashCode() : 0);
        result = 31 * result + (assigned != null ? assigned.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (materialType != null ? materialType.hashCode() : 0);
        result = 31 * result + (tasks != null ? tasks.hashCode() : 0);
        result = 31 * result + (weekCode != null ? weekCode.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (creator != null ? creator.hashCode() : 0);
        result = 31 * result + (publisher != null ? publisher.hashCode() : 0);
        result = 31 * result + (recordId != null ? recordId.hashCode() : 0);
        result = 31 * result + (note != null ? note.hashCode() : 0);
        result = 31 * result + (fulltextLink != null ? fulltextLink.hashCode() : 0);
        return result;
    }
}
