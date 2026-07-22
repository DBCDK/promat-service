package dk.dbc.promat.service.connectors;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpGet;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Client for the faust-resolver service.
 */
public class FaustResolver {
    private static final Pattern PID_PATTERN =
            Pattern.compile("^(?<agencyId>\\d+)-basis:(?<faust>[a-zA-Z0-9]+)$");

    private final String baseUrl;
    private final FailSafeHttpClient failSafeHttpClient;

    public record ManifestationsResponse(List<String> manifestations, String trackingId) {}
    public record ManifestationItemsResponse(List<ManifestationItem> items, String trackingId) {}
    public record ManifestationItem(String manifestationId, int agencyId, String localId, String isbnIssn, String barcode) {}

    public FaustResolver(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        if (failSafeHttpClient == null || baseUrl == null) {
            throw new NullPointerException(String.format(
                    "No parameters is allowed to be null in call to FaustResolver(%s, %s)",
                    failSafeHttpClient == null ? "null" : failSafeHttpClient.toString(),
                    baseUrl == null ? "null" : baseUrl));
        }
        this.failSafeHttpClient = failSafeHttpClient;
        this.baseUrl = baseUrl;
    }

    public void close() {
        failSafeHttpClient.getClient().close();
    }

    /**
     * Resolves an id (either faust or isbn/issn) to a set of fausts.
     * @param id faust or isbn/issn or barcode
     * @return set of fausts
     * @throws FaustResolverException
     */
    public Set<String> resolve(String id) throws FaustResolverException {
        Set<String> manifestations = byOther(IdType.ISBN_ISSN, id);
        manifestations.addAll(byOther(IdType.BARCODE, id));
        if (!manifestations.isEmpty()) {
            return manifestations;
        } else if (faustExists(id)) {
            return Set.of(id);
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Parses a pid on the form "{agencyId}-basis:{faust}" into a faust number.
     *
     * @param pid e.g. "870970-basis:143453731"
     * @return the parsed faust number, or {@code null} if the format does not match
     */
    private static String parsePid(String pid) {
        if (pid == null) {
            return null;
        }
        Matcher m = PID_PATTERN.matcher(pid);
        if (!m.matches()) {
            return null;
        }
        return Objects.equals(m.group("agencyId"), "870970") ? m.group("faust") : null;
    }

    private enum IdType {
        ISBN_ISSN("isbn-issn"), BARCODE("barcode");
        public final String value;
        IdType(String value) {
            this.value = value;
        }
    }
    /**
     * Resolves the manifestations matching a given isbn or issn.
     *
     * @param otherIdType "isbn-issn" or "barcode"
     * @param id isbn or issn number to resolve
     * @return the matching fausts, or {@code null} if none was found
     * @throws FaustResolverException on unexpected failure
     */
    private Set<String> byOther(IdType otherIdType, String id) throws FaustResolverException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("api", "v1", "manifestations", otherIdType.value, id);
        try (Response response = httpGet.execute()) {
            if (response.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
                return Collections.emptySet();
            }
            assertResponseStatus(response);
            return readResponseEntity(response, ManifestationItemsResponse.class)
                    .items.stream()
                    .filter(manifestationItem -> manifestationItem.agencyId == 870970)
                    .map(manifestationItem -> manifestationItem.localId)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Resolves the manifestations belonging to a given faust number.
     *
     * @param faust faust number to resolve
     * @return true if the matching faust was found, false otherwise
     * @throws FaustResolverException on multiple fausts found or unexpected failure
     */
    public boolean faustExists(String faust) throws FaustResolverException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("api","v1", "fausts", faust, "manifestations");
        try (Response response = httpGet.execute()) {
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return false;
            }
            assertResponseStatus(response);
            Set<String> fausts = readResponseEntity(response, ManifestationsResponse.class)
                    .manifestations()
                    .stream()
                    .map(FaustResolver::parsePid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!fausts.isEmpty() && fausts.size() > 1) {
                throw new FaustResolverException("Multiple fausts found for " + faust);
            }
            return fausts.contains(faust);
        }
    }

    private void assertResponseStatus(Response response)
            throws FaustResolverException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (!Response.Status.OK.equals(actualStatus)) {
            throw new FaustResolverException(String.format(
                    "faust-resolver returned with unexpected status code: %s", response.getStatus()));
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> type) throws FaustResolverException {
        final T entity = response.readEntity(type);
        if (entity == null) {
            throw new FaustResolverException(String.format(
                    "faust-resolver returned with null-valued %s entity", type.getName()));
        }
        return entity;
    }
}
