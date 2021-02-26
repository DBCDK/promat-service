/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import javax.xml.bind.annotation.XmlAttribute;

public class XmlCaseviewRequestArg {

    @XmlAttribute
    private String type = "faustno";

    @JacksonXmlText
    private String faustno;

    public String getType() {
        return type;
    }

    @JsonIgnore // prevents serialization as child element, already added as text in this element
    public String getFaustno() {
        return faustno;
    }

    public void setFaustno(String faustno) {
        this.faustno = faustno;
    }

    public XmlCaseviewRequestArg withFaustno(String faustno) {
        this.faustno = faustno;
        return this;
    }
}
