/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"title", "author", "publisher", "faust", "weekcode"})

public class XmlCaseviewBibMetadata {

    private String title;

    private String author;

    private String publisher;

    private String faust;

    private XmlCaseviewWeekcode weekcode;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public XmlCaseviewBibMetadata withTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public XmlCaseviewBibMetadata withAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public XmlCaseviewBibMetadata withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public String getFaust() {
        return faust;
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public XmlCaseviewBibMetadata withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public XmlCaseviewWeekcode getWeekcode() {
        return weekcode;
    }

    public void setWeekcode(XmlCaseviewWeekcode weekcode) {
        this.weekcode = weekcode;
    }

    public XmlCaseviewBibMetadata withWeekcode(XmlCaseviewWeekcode weekcode) {
        this.weekcode = weekcode;
        return this;
    }
}
