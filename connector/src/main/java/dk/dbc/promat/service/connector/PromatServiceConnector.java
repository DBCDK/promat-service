/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.connector;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import net.jodah.failsafe.RetryPolicy;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * PromatServiceConnector - Promat service client
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the Promat service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class PromatServiceConnector {
    // TODO: 19/01/2021 re-enable retry policy
    /* Currently retry handling is disabled to retain backwards compatibility
     * with older versions of the FailSafeHttpClient in use in systems using
     * this connector.
     */
    private static final RetryPolicy RETRY_POLICY = new RetryPolicy()
            .withMaxRetries(0);
            //.handle(ProcessingException.class)
            //.handleResultIf(response -> response.getStatus() == 500)
            //.withDelay(Duration.ofSeconds(5))
            //.withMaxRetries(3);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    public PromatServiceConnector(Client client, String promatServiceBaseUrl) {
        this(FailSafeHttpClient.create(client, RETRY_POLICY), promatServiceBaseUrl);
    }

    public PromatServiceConnector(FailSafeHttpClient failSafeHttpClient, String promatServiceBaseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(promatServiceBaseUrl, "promatServiceBaseUrl");
    }

    public void close() {
        failSafeHttpClient.getClient().close();
    }

    /**
     * lists cases
     * @param params list operation paramters
     * @return CaseSummaryList
     * @throws PromatServiceConnectorException on unexpected failure for list operation
     */
    public CaseSummaryList listCases(ListCasesParams params) throws PromatServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("cases");
        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                httpGet.withQueryParameter(param.getKey(), param.getValue());
            }
        }
        final Response response = httpGet.execute();
        assertResponseStatus(response, Response.Status.OK, Response.Status.NOT_FOUND);
        return readResponseEntity(response, CaseSummaryList.class);
    }

    /**
     * Updates an existing case
     * @param caseRequest case update request
     * @return updated case
     * @throws PromatServiceConnectorException on unexpected failure for update operation
     */
    public PromatCase updateCase(int caseID, CaseRequest caseRequest) throws PromatServiceConnectorException {
        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("cases", String.valueOf(caseID))
                .withJsonData(caseRequest);
        final Response response = httpPost.execute();
        assertResponseStatus(response, Response.Status.OK);
        return readResponseEntity(response, PromatCase.class);
    }

    /**
     * Gets an existing case
     * @param caseID case id
     * @return case
     * @throws PromatServiceConnectorException on unexpected failure for get operation
     */
    public PromatCase getCase(int caseID) throws PromatServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("cases", String.valueOf(caseID));
        final Response response = httpGet.execute();
        assertResponseStatus(response, Response.Status.OK);
        return readResponseEntity(response, PromatCase.class);
    }

    private void assertResponseStatus(Response response, Response.Status... expectedStatus)
            throws PromatServiceConnectorException {
        final Response.Status actualStatus =
                Response.Status.fromStatusCode(response.getStatus());
        if (!Arrays.asList(expectedStatus).contains(actualStatus)) {
            if (response.hasEntity()) {
                final ServiceErrorDto serviceError = readResponseEntity(response, ServiceErrorDto.class);
                throw new PromatServiceConnectorUnexpectedStatusCodeException(serviceError, response.getStatus());
            }
            throw new PromatServiceConnectorUnexpectedStatusCodeException(
                    String.format("Promat service returned with unexpected status code: %s", actualStatus),
                    response.getStatus());
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> type)
            throws PromatServiceConnectorException {
        final T entity = response.readEntity(type);
        if (entity == null) {
            throw new PromatServiceConnectorException(
                    String.format("Promat service returned with null-valued %s entity", type.getName()));
        }
        return entity;
    }

    public static class ListCasesParams extends HashMap<String, Object> {
        public enum Key {
            /**
             * ID of editor assigned to cases
             */
            EDITOR("editor"),
            /**
             * Faust number that must be covered by the cases returned
             */
            FAUST("faust"),
            /**
             * View format for returned cases
             */
            FORMAT("format"),
            /**
             * Return cases with an id greater than this number
             */
            FROM("from"),
            /**
             * Maximum number of cases returned
             */
            LIMIT("limit"),
            /**
             * ID of reviewer assigned to cases
             */
            REVIEWER("reviewer"),
            /**
             * One or more case statuses
             */
            STATUS("status"),
            /**
             * Title (or part of) for cases
             */
            TITLE("title"),
            /**
             * Trimmed weekcode for cases
             */
            TRIMMED_WEEKCODE("trimmedWeekcode"),
            /**
             * Trimmed weekcode comparison operator
             */
            TRIMMED_WEEKCODE_OPERATOR("trimmedWeekcodeOperator");

            private final String keyName;

            Key(String keyName) {
                this.keyName = keyName;
            }

            public String getKeyName() {
                return keyName;
            }
        }

        public enum Format {
            EXPORT, SUMMARY;
        }

        public ListCasesParams withEditor(Integer id) {
            return withInteger(Key.EDITOR, id);
        }

        public Integer getEditor() {
            return getInteger(Key.EDITOR);
        }

        public ListCasesParams withFaust(String faust) {
            return withString(Key.FAUST, faust);
        }

        public String getFaust() {
            return getString(Key.FAUST);
        }

        public ListCasesParams withFormat(Format format) {
            putOrRemoveOnNull(Key.FORMAT, format);
            return this;
        }

        public Format getFormat() {
            final Object value = this.get(Key.FORMAT);
            if (value != null) {
                return (Format) value;
            }
            return null;
        }

        public ListCasesParams withFrom(Integer from) {
            return withInteger(Key.FROM, from);
        }

        public Integer getFrom() {
            return getInteger(Key.FROM);
        }

        public ListCasesParams withLimit(Integer limit) {
            return withInteger(Key.LIMIT, limit);
        }

        public Integer getLimit() {
            return getInteger(Key.LIMIT);
        }

        public ListCasesParams withReviewer(Integer id) {
            return withInteger(Key.REVIEWER, id);
        }

        public Integer getReviewer() {
            return getInteger(Key.REVIEWER);
        }

        public ListCasesParams withStatus(CaseStatus status) {
            final String oldValue = getStatus();
            final String newValue;
            if (oldValue != null) {
                newValue = String.join(",", oldValue, status.name());
            } else {
                newValue = status.name();
            }
            putOrRemoveOnNull(Key.STATUS, newValue);
            return this;
        }

        public String getStatus() {
            return getString(Key.STATUS);
        }

        public ListCasesParams withTitle(String title) {
            return withString(Key.TITLE, title);
        }

        public String getTitle() {
            return getString(Key.TITLE);
        }

        public ListCasesParams withTrimmedWeekcode(String trimmedWeekcode) {
            return withString(Key.TRIMMED_WEEKCODE, trimmedWeekcode);
        }

        public String getTrimmedWeekcode() {
            return getString(Key.TRIMMED_WEEKCODE);
        }

        public ListCasesParams withTrimmedWeekcodeOperator(CriteriaOperator trimmedWeekcodeOperator) {
            putOrRemoveOnNull(Key.TRIMMED_WEEKCODE_OPERATOR, trimmedWeekcodeOperator);
            return this;
        }

        public CriteriaOperator getTrimmedWeekcodeOperator() {
            final Object value = this.get(Key.TRIMMED_WEEKCODE_OPERATOR);
            if (value != null) {
                return (CriteriaOperator) value;
            }
            return null;
        }

        private void putOrRemoveOnNull(Key param, Object value) {
            if (value == null) {
                this.remove(param.keyName);
            } else {
                this.put(param.keyName, value);
            }
        }

        private Object get(Key param) {
            return get(param.keyName);
        }

        private ListCasesParams withInteger(Key key, Integer value) {
            putOrRemoveOnNull(key, value);
            return this;
        }

        public Integer getInteger(Key key) {
            final Object value = this.get(key);
            if (value != null) {
                return (Integer) value;
            }
            return null;
        }

        private ListCasesParams withString(Key key, String value) {
            putOrRemoveOnNull(key, value);
            return this;
        }

        public String getString(Key key) {
            final Object value = this.get(key);
            if (value != null) {
                return (String) value;
            }
            return null;
        }
    }
}