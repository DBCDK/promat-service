/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import jakarta.xml.bind.annotation.XmlAttribute;

public class XmlCaseviewBrief {

    @XmlAttribute
    private String faust;

    @JacksonXmlText
    private String text;

    public XmlCaseviewBrief() {}

    public XmlCaseviewBrief(String requestedFaust, String text) {
        this.faust = requestedFaust;
        this.text = text;
    }

    public String getFaust() {
        return faust;
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public XmlCaseviewBrief withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public XmlCaseviewBrief withText(String text) {
        this.text = text;
        return this;
    }
}
