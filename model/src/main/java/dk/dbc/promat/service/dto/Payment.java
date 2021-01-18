/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import com.fasterxml.jackson.annotation.JsonView;
import dk.dbc.promat.service.persistence.CaseView;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;

import java.time.LocalDate;

public class Payment {

    String payCode;

    PayCategory payCategory;

    int count;

    String text;

    @JsonView({CaseView.Summary.class})
    Reviewer reviewer;

    String primaryFaust;

    String relatedFausts;

    String title;

    String weekCode;

    MaterialType materialType;

    LocalDate deadline;

    public String getPayCode() {
        return payCode;
    }

    public void setPayCode(String payCode) {
        this.payCode = payCode;
    }

    public PayCategory getPayCategory() {
        return payCategory;
    }

    public void setPayCategory(PayCategory payCategory) {
        this.payCategory = payCategory;
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

    public String getRelatedFausts() {
        return relatedFausts;
    }

    public void setRelatedFausts(String relatedFausts) {
        this.relatedFausts = relatedFausts;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Payment withPayCode(String payCode) {
        this.payCode = payCode;
        return this;
    }

    public Payment withPayCategory(PayCategory payCategory) {
        this.payCategory = payCategory;
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

    public Payment withRelatedFausts(String relatedFausts) {
        this.relatedFausts = relatedFausts;
        return this;
    }

    public Payment withMaterialType(MaterialType materialType) {
        this.materialType = materialType;
        return this;
    }

    public Payment withDeadline(LocalDate deadline) {
        this.deadline = deadline;
        return this;
    }
}
