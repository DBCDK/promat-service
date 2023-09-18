/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorDto;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {
    @Override
    public Response toResponse(PersistenceException e) {
        if ("23502".equals(getErrorCode(e).orElse("no error code"))) {   // not_null_violation
            return ServiceErrorDto.InvalidRequest("Missing required field in the request data",
                    e.getMessage());
        }
        return ServiceErrorDto.Failed(e.getMessage());
    }

    private Optional<String> getErrorCode(PersistenceException persistenceException) {
        for (Throwable throwable = persistenceException; throwable != null; throwable = throwable.getCause()) {
            if (throwable instanceof PSQLException) {
                final PSQLException psqlException = (PSQLException) throwable;
                final ServerErrorMessage serverErrorMessage = psqlException.getServerErrorMessage();
                return Optional.ofNullable(serverErrorMessage).map(ServerErrorMessage::getSQLState);
            }
        }
        return Optional.empty();
    }
}
