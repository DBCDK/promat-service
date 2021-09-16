package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class XmlCaseviewSubjterms {

    @XmlAttribute
    private String faust;

    @JacksonXmlElementWrapper(useWrapping = false)
    @XmlElement(name = "term")
    private List<String> terms;

    public XmlCaseviewSubjterms() {}

    public XmlCaseviewSubjterms(String faust, List<String> terms) {
        this.faust = faust;
        this.terms = terms;
    }

    public String getFaust() {
        return faust;
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public XmlCaseviewSubjterms withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public XmlCaseviewSubjterms withTerms(List<String> terms) {
        this.terms = terms;
        return this;
    }
}
