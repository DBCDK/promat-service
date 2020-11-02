package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.Subject;

import java.util.Arrays;
import java.util.Objects;

public class CaseRequestDto implements Dto {

    private String title;

    private String details;

    private String primaryFaust;

    private String[] relatedFausts;

    private Integer reviewer = null;

    private Subject[] subjects;

    private String deadline;

    private String assigned;

    private CaseStatus status;

    private MaterialType materialType;

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

    public String[] getRelatedFausts() {
        return relatedFausts;
    }

    public void setRelatedFausts(String[] relatedFausts) {
        this.relatedFausts = relatedFausts;
    }

    public Integer getReviewer() {
        return reviewer;
    }

    public void setReviewer(Integer reviewer) {
        this.reviewer = reviewer;
    }

    public Subject[] getSubjects() {
        return subjects;
    }

    public void setSubjects(Subject[] subjects) {
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

    public CaseRequestDto withRelatedFausts(String[] relatedFausts)
    {
        this.relatedFausts = relatedFausts;
        return this;
    }

    public CaseRequestDto withReviewer(Integer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public CaseRequestDto withSubjects(Subject[] subjects) {
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

    @Override
    public String toString() {
        return "CaseRequestDto{" +
                "title='" + title + '\'' +
                ", details='" + details + '\'' +
                ", primaryFaust='" + primaryFaust + '\'' +
                ", relatedFausts=" + Arrays.toString(relatedFausts) +
                ", reviewer=" + reviewer +
                ", subjects=" + Arrays.toString(subjects) +
                ", deadline='" + deadline + '\'' +
                ", assigned='" + assigned + '\'' +
                ", status=" + status +
                ", materialType=" + materialType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        CaseRequestDto that = (CaseRequestDto) o;
        return reviewer == that.reviewer &&
                Objects.equals(title, that.title) &&
                Objects.equals(details, that.details) &&
                Objects.equals(primaryFaust, that.primaryFaust) &&
                Arrays.equals(relatedFausts, that.relatedFausts) &&
                Arrays.equals(subjects, that.subjects) &&
                Objects.equals(deadline, that.deadline) &&
                Objects.equals(assigned, that.assigned) &&
                status == that.status &&
                materialType == that.materialType;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(title, details, primaryFaust, reviewer, deadline, assigned, status, materialType);
        result = 31 * result + Arrays.hashCode(relatedFausts);
        result = 31 * result + Arrays.hashCode(subjects);
        return result;
    }
}
