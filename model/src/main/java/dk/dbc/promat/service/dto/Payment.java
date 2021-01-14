/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonView;
import dk.dbc.promat.service.persistence.CaseView;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;

public class Payment {

    String payCode;

    String payType;

    int count;

    String text;

    @JsonView({CaseView.CaseSummary.class})
    Reviewer reviewer;

    String primaryFaust;

    String title;

    String weekCode;

    TaskFieldType taskFieldType;

    public String getPayCode() {
        return payCode;
    }

    public void setPayCode(String payCode) {
        this.payCode = payCode;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Reviewer getReviewer() {
        return reviewer;
    }

    public void setReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
    }

    public String getPrimaryFaust() {
        return primaryFaust;
    }

    public void setPrimaryFaust(String primaryFaust) {
        this.primaryFaust = primaryFaust;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWeekCode() {
        return weekCode;
    }

    public void setWeekCode(String weekCode) {
        this.weekCode = weekCode;
    }

    public TaskFieldType getTaskFieldType() {
        return taskFieldType;
    }

    public void setTaskFieldType(TaskFieldType taskFieldType) {
        this.taskFieldType = taskFieldType;
    }

    public Payment withPayCode(String payCode) {
        this.payCode = payCode;
        return this;
    }

    public Payment withPayType(String payType) {
        this.payType = payType;
        return this;
    }

    public Payment withCount(int count) {
        this.count = count;
        return this;
    }

    public Payment withText(String text) {
        this.text = text;
        return this;
    }

    public Payment withReviewer(Reviewer reviewer) {
        this.reviewer = reviewer;
        return this;
    }

    public Payment withPrimaryFaust(String primaryFaust) {
        this.primaryFaust = primaryFaust;
        return this;
    }

    public Payment withTitle(String title) {
        this.title = title;
        return this;
    }

    public Payment withWeekCode(String weekCode) {
        this.weekCode = weekCode;
        return this;
    }

    public Payment withTaskFieldType(TaskFieldType taskFieldType) {
        this.taskFieldType = taskFieldType;
        return this;
    }
}
