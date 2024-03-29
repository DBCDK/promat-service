package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class SubjectNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView({CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class})
    protected Integer id;

    @JsonView({CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class})
    @Column(name = "subject_id")
    protected Integer subjectId;

    @JsonView({CaseView.Case.class, ReviewerView.Summary.class, ReviewerView.Reviewer.class})
    protected String note;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subject_id) {
        this.subjectId = subject_id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public SubjectNote withId(Integer id) {
        this.id = id;
        return this;
    }

    public SubjectNote withNote(String note) {
        this.note = note;
        return this;
    }

    public SubjectNote withSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if( o == null ) {
            return false;
        }
        if (!(o instanceof SubjectNote)) {
            return false;
        }

        SubjectNote subjectNote = (SubjectNote) o;

        if (!subjectId.equals(subjectNote.subjectId)) {
            return false;
        }
        return note.equals(subjectNote.note);
    }

    @Override
    public int hashCode() {
        int result = subjectId.hashCode();
        result = 31 * result + note.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubjectNote{" +
                "id=" + id +
                ", subjectId=" + subjectId +
                ", note='" + note + '\'' +
                '}';
    }
}
