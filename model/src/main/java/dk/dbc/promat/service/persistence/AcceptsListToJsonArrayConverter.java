/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;
import java.util.List;

@Converter
public class AcceptsListToJsonArrayConverter implements AttributeConverter<List<Reviewer.Accepts>, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    public PGobject convertToDatabaseColumn(List<Reviewer.Accepts> accepts) {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");

        try {
            if (accepts != null) {
                pgObject.setValue(JSONB_CONTEXT.marshall(accepts));
            }
            return pgObject;
        } catch (JSONBException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Reviewer.Accepts> convertToEntityAttribute(PGobject pgObject) {
        if (pgObject != null) {
            try {
                final CollectionType collectionType = JSONB_CONTEXT.getTypeFactory()
                        .constructCollectionType(List.class, Reviewer.Accepts.class);
                return (List) JSONB_CONTEXT.unmarshall(pgObject.getValue(), collectionType);
            } catch (JSONBException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}