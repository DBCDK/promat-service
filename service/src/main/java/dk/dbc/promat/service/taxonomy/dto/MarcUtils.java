package dk.dbc.promat.service.taxonomy.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.marc.binding.MarcBinding;

public class MarcUtils {

    private MarcUtils() {}

    public static MarcBinding toMarcBinding(String jsonString) throws JsonProcessingException {
        final String LEADER = "leader";
        final String FIELDS = "fields";
        final String INDICATORS = "indicators";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonString);

        if (rootNode.has(LEADER)) {
            ((ObjectNode) rootNode).remove(LEADER);
        }

        if (rootNode.has(FIELDS) && rootNode.get(FIELDS).isArray()) {
            ArrayNode fieldsNode = (ArrayNode) rootNode.get(FIELDS);
            for (JsonNode fieldNode : fieldsNode) {
                if (fieldNode.has(INDICATORS)) {
                    ((ObjectNode) fieldNode).remove(INDICATORS);
                }
            }
        }
        return mapper.convertValue(rootNode, MarcBinding.class);
    }
}
