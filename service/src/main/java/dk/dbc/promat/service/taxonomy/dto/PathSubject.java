package dk.dbc.promat.service.taxonomy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties({"path"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PathSubject extends Subject{
    List<String> path;

    public List<String> getPath() {
        return path;
    }

    public PathSubject withPath(String... path) {
        this.path = new ArrayList<>(Arrays.asList(path));
        return this;
    }

    public PathSubject withPath(List<String> path) {
        this.path = path;
        return this;
    }

    @Override
    public String toString() {
        return "PathSubject{" +
                "path=" + path +
                ", title='" + title + '\'' +
                ", note=" + note +
                ", oftenUsed=" + oftenUsed +
                ", id=" + id +
                ", ref='" + ref + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PathSubject subject = (PathSubject) o;
        return Objects.equals(path, subject.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }
}
