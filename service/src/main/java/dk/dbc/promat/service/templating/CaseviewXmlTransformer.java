package dk.dbc.promat.service.templating;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.persistence.PromatCase;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.dbc.promat.service.templating.model.XmlCaseview.XmlCaseview;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class CaseviewXmlTransformer {
    private final XmlMapper xmlMapper;

    @Inject
    @ConfigProperty(name = "HOSTNAME", defaultValue = "unknown")
    private String hostname;

    private Boolean indent = true;

    public Boolean getIndent() {
        return indent;
    }

    public void setIndent(Boolean indent) {
        this.indent = indent;
    }

    public CaseviewXmlTransformer withIndent(Boolean indent) {
        this.indent = indent;
        return this;
    }

    public CaseviewXmlTransformer() {
        this(true);
    }

    public CaseviewXmlTransformer(Boolean indent) {
        this.indent = indent;

        final JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(true);
        xmlMapper = new XmlMapper(module);
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.registerModule(new JakartaXmlBindAnnotationModule());
        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(SerializationFeature.INDENT_OUTPUT, this.indent)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public XmlCaseview toCaseView(String requestedFaust, PromatCase promatCase) throws ServiceErrorException {
        return new XmlCaseview().from(hostname, requestedFaust, promatCase);
    }

    public byte[] toXml(String requestedFaust, PromatCase promatCase) throws ServiceErrorException {
        XmlCaseview caseView = toCaseView(requestedFaust, promatCase);
        return toXml(caseView);
    }

    public byte[] toXml(XmlCaseview caseView) throws ServiceErrorException {
        final StringWriter stringWriter = new StringWriter();
        try {
            xmlMapper.writeValue(stringWriter, caseView);
        } catch (IOException e) {
            throw new ServiceErrorException("Failed to transform caseview to xml")
                    .withDetails("Caught IOException: " + e.getMessage())
                    .withHttpStatus(500).withCode(ServiceErrorCode.FAILED);
        } catch(Exception e) {
            throw new ServiceErrorException("Failed to transform caseview to xml")
                    .withDetails("Caught Exception: " + e.getMessage())
                    .withHttpStatus(500).withCode(ServiceErrorCode.FAILED);
        }

        // Add header and return as 8859-1..  Although unorthodox, this is expected by DBCKat,
        // most likely because data is to be used directly in iso-2709 records without reencoding
        String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n";
        xml += stringWriter.toString();
        return xml.getBytes(StandardCharsets.ISO_8859_1);
    }
}
