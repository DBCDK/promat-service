package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(
        name = Subject.GET_SUBJECTS_LIST_NAME,
        query = Subject.GET_SUBJECTS_LIST_QUERY
)
public class Subject {
    public static final String GET_SUBJECTS_LIST_NAME =
            "Subjects.getSubjects";
    public static final String GET_SUBJECTS_LIST_QUERY =
            "SELECT subject FROM Subject subject where subject.id>0 ORDER BY subject.id";

    @Id
    private Integer id;

    private String name;

    private String path;

    private Integer parentId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    @JsonIgnore
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Subject withId(Integer id) {
        this.id = id;
        return this;
    }

    public Subject withName(String name) {
        this.name = name;
        return this;
    }

    public Subject withParentId(Integer parentId) {
        this.parentId = parentId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subject subject = (Subject) o;

        if (!id.equals(subject.id)) return false;
        if (!name.equals(subject.name)) return false;
        if (path != null ? !path.equals(subject.path) : subject.path != null) return false;
        return parentId != null ? parentId.equals(subject.parentId) : subject.parentId == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}
