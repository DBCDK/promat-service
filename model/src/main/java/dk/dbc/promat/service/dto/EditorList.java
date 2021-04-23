package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.Editor;
import java.util.List;

public class EditorList<T extends Editor> {
    private int numFound = 0;
    List<T> editors = null;

    public EditorList<T> withEditors(List<T> editors) {
        if (editors != null) {
            numFound = editors.size();
        } else {
            numFound = 0;
        }
        this.editors = editors;
        return this;
    }

    public int getNumFound() {
        return numFound;
    }

    public List<T> getEditors() {
        return editors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EditorList)) {
            return false;
        }

        EditorList<?> that = (EditorList<?>) o;

        if (numFound != that.numFound) {
            return false;
        }
        return editors != null ? editors.equals(that.editors) : that.editors == null;
    }

    @Override
    public int hashCode() {
        int result = numFound;
        result = 31 * result + (editors != null ? editors.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EditorList{" +
                "numFound=" + numFound +
                ", editors=" + editors +
                '}';
    }
}
