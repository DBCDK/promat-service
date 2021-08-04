package dk.dbc.promat.service.api;

import dk.dbc.audittrace.Action;
import dk.dbc.audittrace.AuditTrace;
import static dk.dbc.audittrace.Action.READ;
import static dk.dbc.audittrace.AuditTrace.accessingToken;
import static dk.dbc.audittrace.AuditTrace.kv;
import static dk.dbc.audittrace.AuditTrace.owningLenderId;

import dk.dbc.audittrace.KeyValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuditLogHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogHandler.class);

    @Inject
    private JsonWebToken callerPrincipal;

    @Inject
    private HttpServletRequest requestContext;

    @Inject
    @ConfigProperty(name = "PROMAT_AGENCY_ID")
    private String PROMAT_AGENCY_ID;

    private final String APP_NAME = "PROMAT";

    public void logTraceReadForToken(String reason, UriInfo uriInfo, Integer culrId, Integer status) {
        AuditTrace.log(APP_NAME, requestContext,
                accessingToken(callerPrincipal.getRawToken()),
                READ,
                owningLenderId(culrId.toString(), PROMAT_AGENCY_ID),
                kv(reason, uriInfo.getPath()),
                kv("Response", status.toString()));
    }

    public void logTraceUpdateForToken(String reason, UriInfo uriInfo, Integer culrId, Map<String, String> changes) {

        List<KeyValue> keyValues = new ArrayList<>();
        keyValues.add(kv(reason, uriInfo.getPath()));
        keyValues.addAll(changes.entrySet().stream()
                .map(entry -> kv(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));

        AuditTrace.log(APP_NAME, requestContext,
                accessingToken(callerPrincipal.getRawToken()),
                Action.UPDATE,
                owningLenderId(culrId.toString(), PROMAT_AGENCY_ID),
                keyValues.toArray(KeyValue[]::new));
    }

    public void logTraceUpdateForToken(String reason, UriInfo uriInfo, Integer culrId, Integer status) {
        AuditTrace.log(APP_NAME, requestContext,
                accessingToken(callerPrincipal.getRawToken()),
                Action.UPDATE,
                owningLenderId(culrId.toString(), PROMAT_AGENCY_ID),
                kv(reason, uriInfo.getPath()),
                kv("Response", status.toString()));
    }
}
