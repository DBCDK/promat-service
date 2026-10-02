package dk.dbc.promat.service.cataloging;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class MarcXchangeBuilder {
    private final List<DataField> fields = new ArrayList<>();

    public MarcXchangeBuilder addField(String tag, List<Subfield> subfields) {
        fields.add(new DataField(tag, subfields));
        return this;
    }

    public String toXml() {
        // Metakompasset builds the same kind of compact MarcXchange record by hand
        // before sending it to update-service. We keep the builder small here too,
        // but escape values because reviewer text/suggestions may contain XML syntax.
        StringBuilder xml = new StringBuilder();
        xml.append("<marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\" type=\"Bibliographic\">\n");
        for(DataField field : fields) {
            xml.append("  <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"").append(escape(field.tag())).append("\">\n");
            field.subfields().stream()
                    .sorted(Comparator.comparing(Subfield::code))
                    .forEach(subfield -> xml.append("    <marcx:subfield code=\"")
                            .append(escape(subfield.code()))
                            .append("\">")
                            .append(escape(subfield.value()))
                            .append("</marcx:subfield>\n"));
            xml.append("  </marcx:datafield>\n");
        }
        xml.append("</marcx:record>");
        return xml.toString();
    }

    static Subfield subfield(String code, String value) {
        // Same newline handling as metakompasset's danmarc2.utils.js: keep the
        // MARC value on one XML line by writing literal "\n" inside the subfield.
        return new Subfield(code, value == null ? "" : value.replace("\n", "\\n"));
    }

    private static String escape(String value) {
        // Required because these strings are inserted directly into XML attributes
        // and text nodes instead of being written by JAXB/Jackson XML.
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    record DataField(String tag, List<Subfield> subfields) {}
    record Subfield(String code, String value) {}
}
