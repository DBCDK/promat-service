package dk.dbc.promat.service.batch;

import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.binding.SubField;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

// The first four cases are the test cases of OpenFormat's has_subjectLaesekompas field, which
// this check replaces.
class CaseInformationUpdaterMetakompasTest {

    @Test
    void moodsIn665AreARegistration() {
        MarcBinding marcBinding = record()
                .addField(field("665", 'n', "observerende"))
                .addField(field("665", 'n', "oplysende"))
                .addField(field("665", 'n', "tankevækkende"));
        assertThat(CaseInformationUpdater.hasMetakompasRegistration(marcBinding), is(true));
    }

    @Test
    void topicIn665IsARegistration() {
        MarcBinding marcBinding = record().addField(field("665", 'i', "istiden"));
        assertThat(CaseInformationUpdater.hasMetakompasRegistration(marcBinding), is(true));
    }

    @Test
    void subjectsOnlyIn666AreNotARegistration() {
        MarcBinding marcBinding = record()
                .addField(field("666", 'n', "observerende"))
                .addField(field("666", 'i', "istiden"))
                .addField(field("666", 'n', "tankevækkende"));
        assertThat(CaseInformationUpdater.hasMetakompasRegistration(marcBinding), is(false));
    }

    @Test
    void empty665SubjectIsNotARegistration() {
        MarcBinding marcBinding = record()
                .addField(field("665", 'n', ""))
                .addField(field("666", 'i', "istiden"))
                .addField(field("666", 'n', "tankevækkende"));
        assertThat(CaseInformationUpdater.hasMetakompasRegistration(marcBinding), is(false));
    }

    @Test
    void metakompassetFieldIsARegistration() {
        // As written by Metakompasset
        MarcBinding marcBinding = record()
                .addField(new DataField("665", "00")
                        .addSubField(new SubField('&', "positiv"))
                        .addSubField(new SubField('&', "lektor"))
                        .addSubField(new SubField('n', "hyggelig")));
        assertThat(CaseInformationUpdater.hasMetakompasRegistration(marcBinding), is(true));
    }

    @Test
    void only665MarkerSubfieldsAreNotARegistration() {
        MarcBinding marcBinding = record()
                .addField(new DataField("665", "00")
                        .addSubField(new SubField('&', "lektor"))
                        .addSubField(new SubField('b', "not a subject subfield")));
        assertThat(CaseInformationUpdater.hasMetakompasRegistration(marcBinding), is(false));
    }

    @Test
    void personSubjectIsNotARegistration() {
        // The false positive seen with fbi-api's dbcVerified: a person subject from regular cataloguing
        MarcBinding marcBinding = record().addField(field("600", 'a', "Estrid Hein"));
        assertThat(CaseInformationUpdater.hasMetakompasRegistration(marcBinding), is(false));
    }

    private static MarcBinding record() {
        return new MarcBinding().addField(field("001", 'a', "47346029"));
    }

    private static DataField field(String tag, char code, String value) {
        return new DataField(tag, "00").addSubField(new SubField(code, value));
    }
}
