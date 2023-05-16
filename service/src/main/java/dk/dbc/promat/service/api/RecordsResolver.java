package dk.dbc.promat.service.api;

import dk.dbc.opensearch.OpensearchConnector;
import dk.dbc.opensearch.OpensearchConnectorException;
import dk.dbc.opensearch.OpensearchQuery;
import dk.dbc.opensearch.OpensearchQueryCombiner;
import dk.dbc.opensearch.model.OpensearchObject;
import dk.dbc.opensearch.model.OpensearchResult;
import dk.dbc.opensearch.model.OpensearchSearchResponse;
import dk.dbc.opensearch.model.OpensearchSearchResult;
import dk.dbc.promat.service.dto.Dto;
import dk.dbc.promat.service.dto.RecordDto;
import dk.dbc.promat.service.dto.RecordMaterialTypeDto;
import dk.dbc.promat.service.dto.RecordsListDto;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.MaterialType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RecordsResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Records.class);

    @Inject
    OpensearchConnector opensearchConnector;

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
                return new RecordsListDto().withNumFound(0).withRecords(Collections.emptyList());
            }
            LOGGER.info("Got {} opensearch results ", result.hitCount);

            List<RecordDto> results = new ArrayList<RecordDto>();
            for(OpensearchSearchResult searchResult : result.getSearchResult()) {
                for(OpensearchObject searchObject : searchResult.getCollection().getObject()) {
                    String faust = searchObject.getCollection().getRecord().getDatafield("001").getSubfield("a").getValue();
                    String agency = searchObject.getCollection().getRecord().getDatafield("001").getSubfield("b").getValue();

                    String sfa = searchObject.getCollection().getRecord().getDatafield("009").getSubfield("a").getValue();
                    String sfg = searchObject.getCollection().getRecord().getDatafield("009").getSubfield("g").getValue();

                    if(!faust.isEmpty() && !agency.isEmpty()) {
                        LOGGER.info("Adding faust {} in agency {}", faust, agency);
                        results.add(new RecordDto()
                                .withFaust(faust)
                                .withPrimary(faust.equals(id))
                                .withTypes(List.of(mapRrType(sfa, sfg))));
                    }
                }
            }

            LOGGER.info("Id resolved into a list of {} manifestation", results.size());
            return new RecordsListDto()
                    .withNumFound(results.size())
                    .withRecords(results);
        } catch(OpensearchConnectorException opensearchConnectorException) {
            LOGGER.error("Caught OpensearchConnectorException: {}", opensearchConnectorException.getMessage());
            throw opensearchConnectorException;
        }
    }

    private RecordMaterialTypeDto mapRrType(String sfa, String sfg) {
        if (sfa == null || sfg == null || sfa.isEmpty() || sfg.isEmpty()) {
            return new RecordMaterialTypeDto().withMaterialType(MaterialType.UNKNOWN);
        }

        switch(sfa) {
            case "a":
            case "r": return new RecordMaterialTypeDto().withMaterialType(MaterialType.BOOK).withSpecificType(sfa + " " + sfg);
            case "m": return new RecordMaterialTypeDto().withMaterialType(MaterialType.MOVIE).withSpecificType(sfa + " " + sfg);
            case "t": return new RecordMaterialTypeDto().withMaterialType(MaterialType.MULTIMEDIA).withSpecificType(sfa + " " + sfg);
            default: return new RecordMaterialTypeDto().withMaterialType(MaterialType.UNKNOWN).withSpecificType(sfa + " " + sfg);
        }
    }
}
