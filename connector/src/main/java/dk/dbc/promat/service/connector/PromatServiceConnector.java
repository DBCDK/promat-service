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
import dk.dbc.promat.service.dto.ListCasesParams;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.dto.TagList;
import dk.dbc.promat.service.persistence.PromatCase;
import net.jodah.failsafe.RetryPolicy;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
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
    /* Currently retry handling is disabled to retain backwards compatibility
     * with older versions of the FailSafeHttpClient in use in systems using
     * this connector.
     */
    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 500)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

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

    /**
     * Gets an existing case as a html view, suitable for embedded display in DBCKat a.o.
     * Document is expected to be in UTF-8
     * @param faust Faust number
     * @param format Document format (HTML|XML)
     * @return caseview
     * @throws PromatServiceConnectorException on unexpected failure for get operation
     */
    public String getCaseview(String faust, String format) throws PromatServiceConnectorException {
        return getCaseview(faust, format, false, StandardCharsets.UTF_8);
    }

    /**
     * Gets an existing case as a html view by overriding the status check so than any
     * case can be viewed no matter which status it is in
     * Document is expected to be in UTF-8
     * @param faust Faust number
     * @param format Document format (HTML|XML)
     * @return caseview
     * @throws PromatServiceConnectorException on unexpected failure for get operation
     */
    public String getCaseviewWithOverride(String faust, String format) throws PromatServiceConnectorException {
        return getCaseview(faust, format, true, StandardCharsets.UTF_8);
    }

    /**
     * Gets an existing case as a html view, suitable for embedded display in DBCKat a.o.
     * @param faust Faust number
     * @param format Document format (HTML|XML)
     * @param charset Document charset
     * @return caseview
     * @throws PromatServiceConnectorException on unexpected failure for get operation
     */
    public String getCaseview(String faust, String format, Charset charset) throws PromatServiceConnectorException {
        return getCaseview(faust, format, false, charset);
    }

    /**
     * Gets an existing case as a html view, suitable for embedded display in DBCKat a.o.
     * @param faust Faust number
     * @param format Document format (HTML|XML)
     * @param override Override status check (allow fetching view of case not in status APPROVED or better)
     * @param charset Document charset
     * @return caseview
     * @throws PromatServiceConnectorException on unexpected failure for get operation
     */
    public String getCaseview(String faust, String format, boolean override, Charset charset) throws PromatServiceConnectorException {
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("cases", format, faust)
                .withQueryParameter("override", override);
        final Response response = httpGet.execute();
        assertResponseStatus(response, Response.Status.OK);
        byte[] view = readResponseEntity(response, byte[].class);
        return new String(view, charset);
    }


    public PromatCase createDraft(CaseRequest caseRequest) throws PromatServiceConnectorException {
        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("drafts")
                .withJsonData(caseRequest);
        final Response response = httpPost.execute();
        assertResponseStatus(response, Response.Status.CREATED);
        return readResponseEntity(response, PromatCase.class);
    }

    public PromatCase approveBuggiTask(int pid, TagList tags) throws PromatServiceConnectorException {
        HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("cases", Integer.toString(pid), "buggi")
                .withJsonData(tags);
        Response response = httpPost.execute();
        assertResponseStatus(response, Response.Status.OK, Response.Status.NOT_MODIFIED);
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

}
