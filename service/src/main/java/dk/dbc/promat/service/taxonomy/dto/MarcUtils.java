package dk.dbc.promat.service.taxonomy.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.marc.binding.MarcBinding;


/**
 * MarcUtils
 * For regular dm2/dm3 records the way forward would be to use the JsonReader class.
 * Unfortunately, the v2 "dump" endpoint returns the properties "indicator" and "leader" as strings,
 * rather than arrays. This class provides a workaround for that.
 */
public class MarcUtils {
    static final ObjectMapper mapper = new ObjectMapper();
    private MarcUtils() {}

    public static MarcBinding toMarcBinding(String jsonString) throws JsonProcessingException {
        final String LEADER = "leader";
        final String FIELDS = "fields";
        final String INDICATORS = "indicators";

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
