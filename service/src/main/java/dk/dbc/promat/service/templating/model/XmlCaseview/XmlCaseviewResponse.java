/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.promat.service.templating.model.XmlCaseview;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.time.LocalDateTime;

public class XmlCaseviewResponse {

    @XmlAttribute(name = "date")
    private LocalDateTime datetime = LocalDateTime.now();

    @XmlAttribute
    private String server;

    @XmlElement(name = "request_arg")
    private XmlCaseviewRequestArg requestArg;

    public LocalDateTime getDate() {
        return datetime;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public XmlCaseviewResponse withServer(String server) {
        this.server = server;
        return this;
    }

    public XmlCaseviewRequestArg getRequestArg() {
        return requestArg;
    }

    public void setRequestArg(XmlCaseviewRequestArg requestArg) {
        this.requestArg = requestArg;
    }

    public XmlCaseviewResponse withRequestArg(XmlCaseviewRequestArg requestArg) {
        this.requestArg = requestArg;
        return this;
    }
}
