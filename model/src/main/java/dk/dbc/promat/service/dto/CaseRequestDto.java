/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.Editor;
import dk.dbc.promat.service.persistence.MaterialType;

import java.util.List;
import java.util.Objects;

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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        CaseRequestDto that = (CaseRequestDto) o;
        return title.equals(that.title) &&
                Objects.equals(details, that.details) &&
                primaryFaust.equals(that.primaryFaust) &&
                Objects.equals(relatedFausts, that.relatedFausts) &&
                Objects.equals(reviewer, that.reviewer) &&
                Objects.equals(editor, that.editor) &&
                Objects.equals(subjects, that.subjects) &&
                Objects.equals(deadline, that.deadline) &&
                Objects.equals(assigned, that.assigned) &&
                status == that.status &&
                materialType == that.materialType &&
                Objects.equals(tasks, that.tasks) &&
                Objects.equals(weekCode, that.weekCode) &&
                Objects.equals(author, that.author) &&
                Objects.equals(creator, that.creator) &&
                Objects.equals(publisher, that.publisher);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(title, details, primaryFaust, relatedFausts, reviewer, editor, subjects, deadline, assigned, status, materialType, tasks, weekCode, author, creator, publisher);
        return result;
    }
}
