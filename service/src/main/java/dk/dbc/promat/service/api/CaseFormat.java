package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorDto;
import dk.dbc.promat.service.persistence.CaseView;

import jakarta.ws.rs.WebApplicationException;

public enum CaseFormat {
    EXPORT(CaseView.Export.class),
    SUMMARY(CaseView.Summary.class);

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
