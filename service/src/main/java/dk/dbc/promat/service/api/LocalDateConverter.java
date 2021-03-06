/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import javax.ws.rs.ext.ParamConverter;
import java.time.LocalDate;

public class LocalDateConverter implements ParamConverter<LocalDate> {
    @Override
    public LocalDate fromString(String value) {
        if (value == null) {
            return null;
        }
        return LocalDate.parse(value);
    }

    @Override
    public String toString(LocalDate value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
