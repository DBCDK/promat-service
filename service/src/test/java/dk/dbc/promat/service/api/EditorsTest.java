package dk.dbc.promat.service.api;

import dk.dbc.promat.service.persistence.Editor;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class EditorsTest {

    @Test
    void editorEquals() {
        final Editor expectedEditor = new Editor();
        expectedEditor.setId(10);
        expectedEditor.setCulrId("1123-456-789");
        expectedEditor.setActive(true);
        expectedEditor.setFirstName("Ed");
        expectedEditor.setLastName("Itor");
        expectedEditor.setEmail("ed.itor@dbc.dk");
        expectedEditor.setActiveChanged(Date.from(Instant.ofEpochSecond(1629900636)));
        expectedEditor.setAgency("790900");
        expectedEditor.setUserId("jcba");

        final Editor actual = new Editor();
        actual.setId(expectedEditor.getId());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setCulrId(expectedEditor.getCulrId());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setActive(expectedEditor.isActive());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setFirstName(expectedEditor.getFirstName());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setLastName(expectedEditor.getLastName());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setEmail(expectedEditor.getEmail());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setActiveChanged(expectedEditor.getActiveChanged());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setAgency(expectedEditor.getAgency());
        assertThat(actual, is(not(expectedEditor)));
        assertThat(expectedEditor, is(not(actual)));

        actual.setUserId(expectedEditor.getUserId());
        assertThat(actual, is(expectedEditor));
        assertThat(expectedEditor, is(actual));
    }
}
