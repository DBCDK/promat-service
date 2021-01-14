/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Set;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

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

        // Register simple filters (to allow exclude annotations)
        objectMapper.setFilterProvider(new SimpleFilterProvider()
                .addFilter("idAndName", SimpleBeanPropertyFilter
                        .filterOutAllExcept(Set.of("id", "firstName", "lastName")))
                .addFilter("idAndTitle", SimpleBeanPropertyFilter
                        .filterOutAllExcept(Set.of("id", "title")))
        );
    }

    @Override
    public ObjectMapper getContext(Class<?> aClass) {
        return objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}