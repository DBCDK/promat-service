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
public class JsonStringArrayConverter implements AttributeConverter<List<String>, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(List<String> strings) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            if (strings != null) {
                pgObject.setValue(JSONB_CONTEXT.marshall(strings));
            }
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public List<String> convertToEntityAttribute(PGobject pgObject) {
        if (pgObject != null) {
            try {
                final CollectionType collectionType = JSONB_CONTEXT.getTypeFactory()
                        .constructCollectionType(List.class, String.class);
                return JSONB_CONTEXT.unmarshall(pgObject.getValue(), collectionType);
            } catch (JSONBException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}
