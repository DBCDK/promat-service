/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import dk.dbc.commons.jpa.converter.EnumConverter;

import jakarta.persistence.Converter;

@Converter
public class RoleConverter extends EnumConverter<PromatUser.Role> {
    @Override
    public Class<PromatUser.Role> getClassForType() {
        return PromatUser.Role.class;
    }
}
