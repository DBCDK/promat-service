/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces({ MediaType.APPLICATION_JSON })
public class JsonMapperProvider implements ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    public JsonMapperProvider() {
        objectMapper = new ObjectMapper();

        // Register module that knows how to serialize java.time objects
        // Provided by jackson-datatype-jsr310
        objectMapper.registerModule(new JavaTimeModule());

        // Ask Jackson to serialize dates as String (ISO-8601 by default)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ObjectMapper getContext(Class<?> aClass) {
        return objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
