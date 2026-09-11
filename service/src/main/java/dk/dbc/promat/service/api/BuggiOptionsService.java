package dk.dbc.promat.service.api;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Buggi's option vocabulary (4 groups, 22 options total) has no external source - nothing
// syncs it and no admin screen edits it - so it's hardcoded directly here.
@Stateless
@Path("buggi")
public class BuggiOptionsService {

    private static final List<Map<String, Object>> OPTIONS = List.of(
            group("Læsbarhed", "s", false, "let/svær", "tekst/tegninger", "kort/lang"),
            group("Fantasi/virkelighed", "u", false, "virkelig/fantasi"),
            group("Stemning", "n", true,
                    "rar", "sjov", "romantisk", "spændende", "trist", "uhyggelig", "tankevækkende"),
            group("Tema", "e", true,
                    "dyr", "sport", "venskaber", "mit liv", "ud i fremtiden", "skæve karakterer",
                    "den store verden", "fantasy", "gys", "eventyrlig", "action", "gaming")
    );

    @GET
    @Path("options")
    @Produces("application/json")
    public Response getOptions() {
        return Response.ok().entity(OPTIONS).build();
    }

    // LinkedHashMap, not a plain HashMap, so the JSON fields come out in a fixed order every
    // time: "name", "subfieldCode", "requiresNonzeroValue", "options".
    private static Map<String, Object> group(String name, String subfieldCode, boolean requiresNonzeroValue, String... options) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("subfieldCode", subfieldCode);
        entry.put("requiresNonzeroValue", requiresNonzeroValue);
        entry.put("options", List.of(options));
        return entry;
    }
}
