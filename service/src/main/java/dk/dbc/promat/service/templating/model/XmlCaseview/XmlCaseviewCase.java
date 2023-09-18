package dk.dbc.promat.service.templating.model.XmlCaseview;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@JsonPropertyOrder({"caseMetadata", "bibMetadata", "data"})
public class XmlCaseviewCase {

    @XmlAttribute(name = "caseid")
    private Integer caseId;

    @XmlElement(name = "case_metadata")
    private XmlCaseviewCaseMetadata caseMetadata;

    @XmlElement(name = "bib_metadata")
    private XmlCaseviewBibMetadata bibMetadata;

    private XmlCaseviewData data;

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public XmlCaseviewCase withCaseId(Integer caseId) {
        this.caseId = caseId;
        return this;
    }

    public XmlCaseviewCaseMetadata getCaseMetadata() {
        return caseMetadata;
    }

    public void setCaseMetadata(XmlCaseviewCaseMetadata caseMetadata) {
        this.caseMetadata = caseMetadata;
    }

    public XmlCaseviewCase withCaseMetadata(XmlCaseviewCaseMetadata caseMetadata) {
        this.caseMetadata = caseMetadata;
        return this;
    }

    public XmlCaseviewBibMetadata getBibMetadata() {
        return bibMetadata;
    }

    public void setBibMetadata(XmlCaseviewBibMetadata bibMetadata) {
        this.bibMetadata = bibMetadata;
    }

    public XmlCaseviewCase withBibMetadata(XmlCaseviewBibMetadata bibMetadata) {
        this.bibMetadata = bibMetadata;
        return this;
    }

    public XmlCaseviewData getData() {
        return data;
    }

    public void setData(XmlCaseviewData data) {
        this.data = data;
    }

    public XmlCaseviewCase withData(XmlCaseviewData data) {
        this.data = data;
        return this;
    }
}
