/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"bkmeval", "brief", "description", "evaluation", "comparison", "recommendation", "age", "matlevel", "subjterm"})

public class XmlCaseviewData {

    private String bkmeval;

    private XmlCaseviewBrief brief;

    private String description;

    private String evaluation;

    private String comparison;

    private String recommendation;

    private String age;

    private XmlCaseviewMatlevels matlevel;

    private XmlCaseviewSubjterms subjterm;

    public String getBkmeval() {
        return bkmeval;
    }

    public void setBkmeval(String bkmeval) {
        this.bkmeval = bkmeval;
    }

    public XmlCaseviewData withBkmeval(String bkmeval) {
        this.bkmeval = bkmeval;
        return this;
    }

    public XmlCaseviewBrief getBrief() {
        return brief;
    }

    public void setBrief(XmlCaseviewBrief brief) {
        this.brief = brief;
    }

    public XmlCaseviewData withBrief(XmlCaseviewBrief brief) {
        this.brief = brief;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public XmlCaseviewData withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public XmlCaseviewData withEvaluation(String evaluation) {
        this.evaluation = evaluation;
        return this;
    }

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
    }

    public XmlCaseviewData withComparison(String comparison) {
        this.comparison = comparison;
        return this;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public XmlCaseviewData withRecommendation(String recommendation) {
        this.recommendation = recommendation;
        return this;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public XmlCaseviewData withAge(String age) {
        this.age = age;
        return this;
    }

    public XmlCaseviewMatlevels getMatlevel() {
        return matlevel;
    }

    public void setMatlevel(XmlCaseviewMatlevels matlevel) {
        this.matlevel = matlevel;
    }

    public XmlCaseviewData withMatlevel(XmlCaseviewMatlevels matlevel) {
        this.matlevel = matlevel;
        return this;
    }

    public XmlCaseviewSubjterms getSubjterm() {
        return subjterm;
    }

    public void setSubjterm(XmlCaseviewSubjterms subjterm) {
        this.subjterm = subjterm;
    }

    public XmlCaseviewData withSubjterm(XmlCaseviewSubjterms subjterm) {
        this.subjterm = subjterm;
        return this;
    }
}
