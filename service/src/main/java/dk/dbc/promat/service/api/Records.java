/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.opensearch.OpensearchConnector;
import dk.dbc.opensearch.OpensearchConnectorException;
import dk.dbc.opensearch.OpensearchQuery;
import dk.dbc.opensearch.OpensearchQueryCombiner;
import dk.dbc.opensearch.model.OpensearchObject;
import dk.dbc.opensearch.model.OpensearchResult;
import dk.dbc.opensearch.model.OpensearchSearchResponse;
import dk.dbc.opensearch.model.OpensearchSearchResult;
import dk.dbc.opensearch.workpresentation.WorkPresentationConnector;
import dk.dbc.opensearch.workpresentation.WorkPresentationConnectorException;
import dk.dbc.opensearch.workpresentation.WorkPresentationQuery;
import dk.dbc.opensearch.workpresentation.model.WorkPresentationGroup;
import dk.dbc.opensearch.workpresentation.model.WorkPresentationRecord;
import dk.dbc.opensearch.workpresentation.model.WorkPresentationWork;
import dk.dbc.promat.service.dto.Dto;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordMaterialTypeDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.MaterialType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Stateless
@Path("records")
public class Records {
    private static final Logger LOGGER = LoggerFactory.getLogger(Records.class);

    @Inject
    public OpensearchConnector opensearchConnector;

    @Inject
    public WorkPresentationConnector workPresentationConnector;

    public Dto resolveId(String id) throws Exception{
        try {

            // Get records that either match directly on the id (faust) or with the id as the isbn number
            LOGGER.info("Opensearch for id={} or is={}", id, id);
            OpensearchSearchResponse response = opensearchConnector.search(new OpensearchQuery()
                    .withCombiner(OpensearchQueryCombiner.OR)
                    .withId(id)
                    .withIs(id)
                    .withBc(id));

            if(!response.getError().isEmpty()) {
                LOGGER.info("Opensearch error: {}", response.getError());
                return new ServiceErrorDto().withCode(ServiceErrorCode.FAILED).withCause("Opensearch error").withDetails(response.getError());
            }

            OpensearchResult result = response.getResult();
            if( result.hitCount == 0 ) {
                LOGGER.info("No results from opensearch. Id is unknown");
                return new RecordsListDto();
            }
            LOGGER.info("Got {} opensearch results ", result.hitCount);

            // Look up the work for every returned faustnumber from the initial search.
            // Often, only a single faustnumber will be returned, but there may be several
            // records having the same isbn number, so given an isbn number - we might get several
            // faustnumbers back.
            List<RecordDto> relatedByWork = new ArrayList<RecordDto>();
            for(OpensearchSearchResult searchResult : result.getSearchResult()) {
                for(OpensearchObject searchObject : searchResult.getCollection().getObject()) {
                    String faust = searchObject.getCollection().getRecord().getDatafield("001").getSubfield("a").getValue();
                    String agency = searchObject.getCollection().getRecord().getDatafield("001").getSubfield("b").getValue();
                    if(!faust.isEmpty() && !agency.isEmpty()) {
                        LOGGER.info("Presenting work for faust {} in agency {}", faust, agency);
                        WorkPresentationWork work = workPresentationConnector.presentWorks(new WorkPresentationQuery()
                                .withAgencyId(agency)
                                .withManifestation(faust));

                        LOGGER.info("Work returned {} manifestations", work.getGroups().length);
                        LOGGER.info("Primary manifestations is {}", work.getGroups().length > 0 ? work.getManifestation() : "(none)");
                        for(WorkPresentationGroup group : work.getGroups()) {
                            for(WorkPresentationRecord record : group.getRecords()) {
                                LOGGER.info("Adding manifestation {}", record.getManifestation());
                                relatedByWork.add(new RecordDto()
                                        .withFaust(record.getManifestation())
                                        .withPrimary(record.getManifestation().equals(work.getManifestation()))
                                        .withTypes(mapRrTypes(record.getTypes())));
                            }
                        }
                    }
                }
            }

            LOGGER.info("Id resolved into a list of {} manifestations related by work", relatedByWork.size());
            return new RecordsListDto()
                    .withNumFound(relatedByWork.size())
                    .withRecords(relatedByWork);
        } catch(OpensearchConnectorException opensearchConnectorException) {
            LOGGER.error("Caught OpensearchConnectorException: {}", opensearchConnectorException.getMessage());
            throw opensearchConnectorException;
        } catch(WorkPresentationConnectorException workPresentationConnectorException) {
            LOGGER.error("Caught WorkPresentationConnectorException: {}", workPresentationConnectorException.getMessage());
            throw workPresentationConnectorException;
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecords(@PathParam("id") final String id) throws Exception {
        LOGGER.info("getRecords/{}", id);

        // Find every record with that belongs to any work that matches the given id
        try {
            Dto dto = resolveId(id);
            if(dto.getClass().equals(RecordsListDto.class)) {
                return Response.ok(dto).build();
            }
            return Response.status(400).entity(dto).build();
        } catch(Exception exception) {
            LOGGER.info("Caught exception: {}", exception.getMessage());
            throw exception;
        }
    }

    private List<RecordMaterialTypeDto> mapRrTypes(String[] rrTypes) {

        // Types are values from the list of danbib materialtype codes.
        // Usually, only a single materialtype is listed, but the list of types
        // is declared as a list by the workpresentation service, so it would
        // be expected that cases exists where a record can be mapped to multiple types
        //
        // https://danbib.dk/docs/abm/types.xml

        // Map known types, ignore the rest
        ArrayList<RecordMaterialTypeDto> types = new ArrayList<>();
        for(String rrType : rrTypes) {
            switch(rrType) {

                case "Billedbog":
                case "Bog":
                case "Bog stor skrift":
                case "Ebog":
                case "Lydbog":
                case "Lydbog (bånd)":
                case "Lydbog (cd)":
                case "Lydbog (net)":
                case "Lydbog (cd-mp3)":
                case "Årbog":
                case "Punktskrift":
                case "DTBook": types.add(new RecordMaterialTypeDto()
                        .withMaterialType(MaterialType.BOOK)
                        .withSpecificType(rrType));
                break;

                case "Biograffilm":
                case "DVD (film)":
                case "Film":
                case "Film (net)":
                case "Blu-ray": types.add(new RecordMaterialTypeDto()
                        .withMaterialType(MaterialType.MOVIE)
                        .withSpecificType(rrType));
                break;

                case "CD":
                case "CD-I":
                case "CD-rom":
                case "Cd-rom (mp3)":
                case "Computerspil":
                case "Dvd":
                case "DVD-rom":
                case "Elektronisk materiale":
                case "GameBoy":
                case "GameBoy Advance":
                case "GameBoy Color":
                case "Nintendo 3":
                case "Nintendo 3DS":
                case "Nintendo DS":
                case "Nintendo Switch":
                case "Pc-spil":
                case "Pc-spil (net)":
                case "Playstation":
                case "Playstation 2 [brugt for PS2]":
                case "Playstation 3 [brugt for PS3]":
                case "Playstation 4 [brugt for PS4]":
                case "Playstation Vita":
                case "Wii":
                case "Wii U":
                case "Xbox":
                case "Xbox 360":
                case "Xbox One": types.add(new RecordMaterialTypeDto()
                        .withMaterialType(MaterialType.MULTIMEDIA)
                        .withSpecificType(rrType))
                ; break;

                default: types.add(new RecordMaterialTypeDto()
                        .withMaterialType(MaterialType.UNKNOWN)
                        .withSpecificType(rrType));
                break;
            }
        }
        return types;
    }
}
