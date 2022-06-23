/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorDto;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import javax.persistence.PersistenceException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
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
