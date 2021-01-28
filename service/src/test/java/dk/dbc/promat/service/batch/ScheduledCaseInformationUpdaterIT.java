/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import dk.dbc.promat.service.ContainerTest;
import dk.dbc.promat.service.IntegrationTest;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.MaterialType;
import dk.dbc.promat.service.persistence.PromatCase;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ScheduledCaseInformationUpdaterIT extends ContainerTest {

    @Test
    public void testUpdateCaseFromOpenformat() throws JsonProcessingException {

        // Todo: wiremock some data from opensearch/openformat

        CaseInformationUpdater updater = new CaseInformationUpdater();

        // Create a case with incorrect title and weekcode
        CaseRequest dto = new CaseRequest()
                .withPrimaryFaust("24699773")  // in 870970
                .withTitle("Title for 24699773")
                .withWeekCode("DPF202102") // Todo: Modify this in the mock data, it must not be less than 202101 due to other tests
                .withDetails("Details for 24699773")
                .withMaterialType(MaterialType.BOOK)
                .withAssigned("2021-01-28")
                .withDeadline("2024-02-29")
                .withCreator(10)
                .withEditor(10)
                .withReviewer(1);

        Response response = postResponse("v1/api/cases", dto);
        assertThat("status code", response.getStatus(), is(201));
        PromatCase created = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Run scheduled updater
        updater.updateCaseInformation(created);

        // Check that the case has been updated
        response = getResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
        PromatCase updated = mapper.readValue(response.readEntity(String.class), PromatCase.class);

        // Todo: Check against correct openformat values
        //assertThat("title is correct", updated.getTitle(), is("something-something"));
        //assertThat("weekcode is correct", updated.getWeekCode(), is("SOMething"));

        // Delete the case so that we dont mess up payments tests
        response = deleteResponse("v1/api/cases/" + created.getId());
        assertThat("status code", response.getStatus(), is(200));
    }
}
