package dk.dbc.promat.service.templating.model.XmlCaseview;

public class XmlCaseviewWeekcode {

    private String code;

    public XmlCaseviewWeekcode() {}

    public XmlCaseviewWeekcode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public XmlCaseviewWeekcode withCode(String code) {
        this.code = code;
        return this;
    }
}
