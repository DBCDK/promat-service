/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseRequestDto implements Dto {

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

    public CaseRequestDto withTitle(String title) {
        this.title = title;
        return this;
    }

    public CaseRequestDto withDetails(String details) {
        this.details = details;
        return this;
    }

    public CaseRequestDto withPrimaryFaust(String primaryFaust) {
        this.primaryFaust = primaryFaust;
        return this;
    }

    public CaseRequestDto withRelatedFausts(List<String> relatedFausts)
    {
        this.relatedFausts = relatedFausts;
        return this;
    }

    public CaseRequestDto withReviewer(Integer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public CaseRequestDto withEditor(Integer editor) {
        this.editor = editor;
        return this;
    }

    public CaseRequestDto withSubjects(List<Integer> subjects) {
        this.subjects = subjects;
        return this;
    }

    public CaseRequestDto withDeadline(String deadline) {
        this.deadline = deadline;
        return this;
    }

    public CaseRequestDto withAssigned(String assigned) {
        this.assigned = assigned;
        return this;
    }

    public CaseRequestDto withStatus(CaseStatus status) {
        this.status = status;
        return this;
    }

    public CaseRequestDto withMaterialType(MaterialType materialType) {
        this.materialType = materialType;
        return this;
    }

    public CaseRequestDto withTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
        return this;
    }

    public CaseRequestDto withWeekCode(String weekCode) {
        this.weekCode = weekCode;
        return this;
    }

    public CaseRequestDto withAuthor(String author) {
        this.author = author;
        return this;
    }

    public CaseRequestDto withCreator(Integer creator) {
        this.creator = creator;
        return this;
    }

    public CaseRequestDto withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public CaseRequestDto withRecordId(String recordId) {
        setRecordId(recordId);
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

        CaseRequestDto that = (CaseRequestDto) o;

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
        return recordId != null ? recordId.equals(that.recordId) : that.recordId == null;
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
        return result;
    }
}
