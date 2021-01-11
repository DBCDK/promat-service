/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.Subject;
import java.util.List;

public class SubjectList {
    int numFound = 0;
    List<Subject> subjects = null;

    public SubjectList withSubjects(List<Subject> subjects) {
        if (subjects != null) {
            numFound = subjects.size();
        } else {
            numFound = 0;
        }
        this.subjects = subjects;
        return this;
    }

    public int getNumFound() {
        return numFound;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubjectList that = (SubjectList) o;

        if (numFound != that.numFound) return false;
        return subjects != null ? subjects.equals(that.subjects) : that.subjects == null;
    }

    @Override
    public int hashCode() {
        int result = numFound;
        result = 31 * result + (subjects != null ? subjects.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SubjectList{" +
                "numFound=" + numFound +
                ", subjects=" + subjects +
                '}';
    }
}
