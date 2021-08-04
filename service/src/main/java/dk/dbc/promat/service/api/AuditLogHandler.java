package dk.dbc.promat.service.api;

import dk.dbc.audittrace.AccessingUser;
import dk.dbc.audittrace.Action;
import dk.dbc.audittrace.KeyValue;
import dk.dbc.audittrace.OwningUser;
import static dk.dbc.audittrace.Action.READ;
import static dk.dbc.audittrace.AuditTrace.accessingToken;
import static dk.dbc.audittrace.AuditTrace.kv;
import static dk.dbc.audittrace.AuditTrace.owningLenderId;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

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

    public void logTraceForToken(String reason, String path, Integer culrId, Integer status) {

        AccessingUser accessingUser = accessingToken(callerPrincipal.getRawToken());
        Action action = READ;
        OwningUser owningUser = owningLenderId(culrId.toString(), PROMAT_AGENCY_ID);
        KeyValue keyValueReason = kv(reason, path);
        KeyValue keyValueStatus = kv("Response", status.toString());

        // Todo: BEGIN__ Temporary log message to verify that we have valid parameters for audit trail logging
        LOGGER.info("Raw token: {}", callerPrincipal.getRawToken());
        LOGGER.info("culrId: {}", culrId);
        LOGGER.info("appName: {}", APP_NAME);
        LOGGER.info("requestContext: {}", requestContext);
        LOGGER.info("action: {}", action);
        LOGGER.info("owningUser: {}", owningUser);
        LOGGER.info("accesingUser: {}", accessingUser);
        LOGGER.info("keyValueReason: {}", keyValueReason);
        LOGGER.info("keyValueStatus: {}", keyValueStatus);
        // Example: AuditTrace.log("app_name", requestContext, userToken(token), READ, owningLenderId("1234", "710100"), kv("app_key", "app_value"));
        // Todo: __END remove before going into production

        // Todo: Enable audit log call
        // Real call: AuditTrace.log(APP_NAME, requestContext, accessingUser, action, owningUser, keyValueReason, keyValueStatus);
        LOGGER.info("AuditTrace({} @{} for {} = {})", reason, path, culrId, status);
    }
}
