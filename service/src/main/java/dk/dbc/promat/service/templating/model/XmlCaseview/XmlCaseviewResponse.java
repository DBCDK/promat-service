package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.time.LocalDateTime;


public class XmlCaseviewResponse {

    @XmlAttribute(name = "date")
    private LocalDateTime datetime = LocalDateTime.now();

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }

    public XmlCaseviewResponse withDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
        return this;
    }

    @XmlAttribute
    private String server;

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

    @XmlElement(name = "request_arg")
    private XmlCaseviewRequestArg requestArg;


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
