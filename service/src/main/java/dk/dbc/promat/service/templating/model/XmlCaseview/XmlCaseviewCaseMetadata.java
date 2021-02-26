/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDate;

@JsonPropertyOrder({"deadline", "casestate", "user", "note"})

public class XmlCaseviewCaseMetadata {

    private LocalDate deadline;

    private String casestate;

    private String user;

    private String note;

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public XmlCaseviewCaseMetadata withDeadline(LocalDate deadline) {
        this.deadline = deadline;
        return this;
    }

    public String getCasestate() {
        return casestate;
    }

    public void setCasestate(String casestate) {
        this.casestate = casestate;
    }

    public XmlCaseviewCaseMetadata withCasestate(String casestate) {
        this.casestate = casestate;
        return this;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public XmlCaseviewCaseMetadata withUser(String user) {
        this.user = user;
        return this;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public XmlCaseviewCaseMetadata withNote(String note) {
        this.note = note;
        return this;
    }
}
