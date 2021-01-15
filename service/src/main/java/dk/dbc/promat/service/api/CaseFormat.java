/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.CaseView;

import javax.ws.rs.WebApplicationException;

public enum CaseFormat {
    EXPORT(CaseView.Export.class),
    SUMMARY(CaseView.Summary.class),
    IDENTITY(CaseView.Identity.class);

    private Class<?> viewClass;

    CaseFormat(Class<?> viewClass) {
        this.viewClass = viewClass;
    }

    public Class<?> getViewClass() {
        return viewClass;
    }

    public static CaseFormat fromString(String param) {
        try {
            return valueOf(param.toUpperCase());
        } catch (Exception e) {
            throw new WebApplicationException(ServiceErrorDto.InvalidRequest(
                    "Illegal case view format: " + param, null));
        }
    }
}
