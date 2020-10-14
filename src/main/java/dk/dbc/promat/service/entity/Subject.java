package dk.dbc.promat.service.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "subject")
@NamedQuery(
        name = Subject.GET_SUBJECTS_LIST_NAME,
        query = Subject.GET_SUBJECTS_LIST_QUERY
)
public class Subject {
    public static final String GET_SUBJECTS_LIST_NAME =
            "Subjects.getSubjects";
    public static final String GET_SUBJECTS_LIST_QUERY =
            "SELECT subject FROM Subject subject ORDER BY subject.id";

    @Id @GeneratedValue
    private long id;

    private String name;

    @Column(name = "parentid")
    private long parentId = -1;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public Subject withId(long id) {
        this.id = id;
        return this;
    }

    public Subject withName(String name) {
        this.name = name;
        return this;
    }

    public Subject withParentId(long parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subject subject = (Subject) o;

        if (id != subject.id) return false;
        if (parentId != subject.parentId) return false;
        return name.equals(subject.name);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + name.hashCode();
        result = 31 * result + (int) (parentId ^ (parentId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}
