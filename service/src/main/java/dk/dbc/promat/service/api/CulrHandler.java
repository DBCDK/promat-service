package dk.dbc.promat.service.api;

import dk.dbc.connector.culr.CulrConnector;
import dk.dbc.connector.culr.CulrConnectorException;
import dk.dbc.culrservice.ws.AuthCredentials;
import dk.dbc.culrservice.ws.CulrResponse;
import dk.dbc.culrservice.ws.GetAccountsByAccountIdResponse;
import dk.dbc.culrservice.ws.GlobalUID;
import dk.dbc.culrservice.ws.GlobalUidTypes;
import dk.dbc.culrservice.ws.ResponseCodes;
import dk.dbc.culrservice.ws.UserIdTypes;
import dk.dbc.culrservice.ws.UserIdValueAndType;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.ServiceErrorDto;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class CulrHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CulrHandler.class);

    @Inject
    @ConfigProperty(name = "PROMAT_AGENCY_ID")
    private String PROMAT_AGENCY_ID;

    @Inject CulrConnector culrConnector;

    @Inject
    @ConfigProperty(name = "CULR_SERVICE_USER_ID")
    private String culrServiceUserId;

    @Inject
    @ConfigProperty(name = "CULR_SERVICE_PASSWORD")
    private String culrServicePassword;

    public String createCulrAccount(String cprNumber, String localId)
            throws CulrConnectorException, ServiceErrorException {
        final AuthCredentials authCredentials = new AuthCredentials();
        authCredentials.setUserIdAut(culrServiceUserId);
        authCredentials.setGroupIdAut(PROMAT_AGENCY_ID);
        authCredentials.setPasswordAut(culrServicePassword);

        final UserIdValueAndType userCredentialsCPR = new UserIdValueAndType();
        userCredentialsCPR.setUserIdType(UserIdTypes.CPR);
        userCredentialsCPR.setUserIdValue(cprNumber);

        final CulrResponse culrCprResponse = culrConnector.createAccount(PROMAT_AGENCY_ID, userCredentialsCPR, authCredentials);
        if (culrCprResponse.getResponseStatus().getResponseCode() == ResponseCodes.OK_200
                || accountExists(userCredentialsCPR, authCredentials)) {
            // Account for CPR number could be created, or maybe it already existed
            // if a previous PROMAT create user operation was interrupted.

            final UserIdValueAndType userCredentialsLocal = new UserIdValueAndType();
            userCredentialsLocal.setUserIdType(UserIdTypes.LOCAL);
            userCredentialsLocal.setUserIdValue(localId);
            final GlobalUID globalUID = new GlobalUID();
            globalUID.setUidType(GlobalUidTypes.CPR);
            globalUID.setUidValue(cprNumber);

            // Attach local ID to account
            final CulrResponse culrLocalResponse = culrConnector.createAccount(
                    PROMAT_AGENCY_ID, userCredentialsLocal, authCredentials, globalUID, null);
            if (culrLocalResponse.getResponseStatus().getResponseCode() != ResponseCodes.OK_200) {
                // Maybe the local ID was already created if a previous PROMAT create
                // user operation was interrupted.

                // Assert that both CPR number and local ID belong to the same account.
                final String guidFromCprLookup = getAccountGuidOrThrow(userCredentialsCPR, authCredentials);
                final String guidFromLocalLookup = getAccountGuidOrThrow(userCredentialsLocal, authCredentials);
                if (!guidFromCprLookup.equals(guidFromLocalLookup)) {
                    final String cause = "CULR ID for CPR number and LOCAL ID lookups did not match";
                    LOGGER.error(cause);
                    throw new ServiceErrorException(cause)
                            .withCode(ServiceErrorCode.EXTERNAL_SERVICE_ERROR)
                            .withCause(cause)
                            .withDetails(String.format("CULR ID for CPR number and LOCAL ID was %s - %s",
                                    guidFromCprLookup, guidFromLocalLookup));
                }
                return guidFromLocalLookup;
            }
            return getAccountGuidOrThrow(userCredentialsLocal, authCredentials);
        }

        final ResponseCodes responseCode = culrCprResponse.getResponseStatus().getResponseCode();
        final String responseMessage = culrCprResponse.getResponseStatus().getResponseMessage();
        LOGGER.error("Attempt to create CULR ID for CPR number failed, response was {} - {}", responseCode, responseMessage);
        throw new ServiceErrorException("Failed to create CULR ID for CPR number")
                .withCode(ServiceErrorCode.EXTERNAL_SERVICE_ERROR)
                .withCause("Failed to create CULR ID for CPR number")
                .withDetails(String.format("CULR service response was %s - %s", responseCode, responseMessage));
    }

    public Optional<ServiceErrorDto> verifyCulrAccount(String culrId, String localId) throws CulrConnectorException {
        final AuthCredentials authCredentials = new AuthCredentials();
        authCredentials.setUserIdAut(culrServiceUserId);
        authCredentials.setGroupIdAut(PROMAT_AGENCY_ID);
        authCredentials.setPasswordAut(culrServicePassword);

        final UserIdValueAndType userCredentialsLocal = new UserIdValueAndType();
        userCredentialsLocal.setUserIdType(UserIdTypes.LOCAL);
        userCredentialsLocal.setUserIdValue(localId);

        final GetAccountsByAccountIdResponse accountResponse = getAccount(userCredentialsLocal, authCredentials);
        if (accountResponse.getResponseStatus().getResponseCode() != ResponseCodes.OK_200) {
            return Optional.of(new ServiceErrorDto()
                    .withCause("User not authorized")
                    .withDetails(String.format("Local ID %s was not found in CULR", localId))
                    .withCode(ServiceErrorCode.NOT_FOUND));
        }
        if (!accountResponse.getGuid().equals(culrId)) {
            return Optional.of(new ServiceErrorDto()
                    .withCause("User not authorized")
                    .withDetails(String.format("Local ID belongs to CULR ID %s whereas Promat has %s",
                            accountResponse.getGuid(), culrId))
                    .withCode(ServiceErrorCode.FORBIDDEN));
        }

        return Optional.empty();
    }

    private GetAccountsByAccountIdResponse getAccount(UserIdValueAndType userCredentials, AuthCredentials authCredentials)
            throws CulrConnectorException {
        return culrConnector.getAccountFromProvider(PROMAT_AGENCY_ID, userCredentials, authCredentials);
    }

    private boolean accountExists(UserIdValueAndType userCredentials, AuthCredentials authCredentials)
            throws CulrConnectorException {
        final GetAccountsByAccountIdResponse account = getAccount(userCredentials, authCredentials);
        return account.getResponseStatus().getResponseCode() == ResponseCodes.OK_200;
    }

    private String getAccountGuidOrThrow(UserIdValueAndType userCredentials, AuthCredentials authCredentials)
            throws CulrConnectorException, ServiceErrorException {
        final GetAccountsByAccountIdResponse accountResponse = getAccount(userCredentials, authCredentials);
        if (accountResponse.getResponseStatus().getResponseCode() == ResponseCodes.OK_200) {
            return accountResponse.getGuid();
        }
        final ResponseCodes responseCode = accountResponse.getResponseStatus().getResponseCode();
        final String responseMessage = accountResponse.getResponseStatus().getResponseMessage();
        LOGGER.error("Attempt to obtain CULR ID of type {} failed, response was {} - {}",
                userCredentials.getUserIdType(), responseCode, responseMessage);
        final String cause = String.format("Failed to obtain CULR ID of type %s", userCredentials.getUserIdType());
        throw new ServiceErrorException(cause)
                .withCode(ServiceErrorCode.EXTERNAL_SERVICE_ERROR)
                .withCause(cause)
                .withDetails(String.format("CULR service response was %s - %s", responseCode, responseMessage));
    }
}
