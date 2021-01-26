/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
                final String guidFromCprLookup = getAccountGuid(userCredentialsCPR, authCredentials);
                final String guidFromLocalLookup = getAccountGuid(userCredentialsLocal, authCredentials);
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
            return getAccountGuid(userCredentialsLocal, authCredentials);
        }

        final ResponseCodes responseCode = culrCprResponse.getResponseStatus().getResponseCode();
        final String responseMessage = culrCprResponse.getResponseStatus().getResponseMessage();
        LOGGER.error("Attempt to create CULR ID for CPR number failed, response was {} - {}", responseCode, responseMessage);
        throw new ServiceErrorException("Failed to create CULR ID for CPR number")
                .withCode(ServiceErrorCode.EXTERNAL_SERVICE_ERROR)
                .withCause("Failed to create CULR ID for CPR number")
                .withDetails(String.format("CULR service response was %s - %s", responseCode, responseMessage));
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

    private String getAccountGuid(UserIdValueAndType userCredentials, AuthCredentials authCredentials)
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
