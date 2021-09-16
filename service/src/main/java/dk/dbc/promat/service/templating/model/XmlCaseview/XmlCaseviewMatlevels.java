package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class XmlCaseviewMatlevels {

    @XmlAttribute
    private String faust;

    @JacksonXmlElementWrapper(useWrapping = false)
    @XmlElement(name = "level")
    private List<String> levels;

    public XmlCaseviewMatlevels() {}

    public XmlCaseviewMatlevels(String faust, List<String> terms) {
        this.faust = faust;
        this.levels = terms;
    }

    public String getFaust() {
        return faust;
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public XmlCaseviewMatlevels withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public List<String> getLevels() {
        return levels;
    }

    public void setLevels(List<String> levels) {
        this.levels = levels;
    }

    public XmlCaseviewMatlevels withTerms(List<String> levels) {
        this.levels = levels;
        return this;
    }
}
