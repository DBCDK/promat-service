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
