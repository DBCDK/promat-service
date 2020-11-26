/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import dk.dbc.commons.jpa.converter.JsonArrayConverter;

import javax.persistence.Converter;

@Converter
public class AcceptsListToJsonArrayConverter extends JsonArrayConverter<Reviewer.Accepts> {
    @Override
    public Class<Reviewer.Accepts> getClassForType() {
        return Reviewer.Accepts.class;
    }
}