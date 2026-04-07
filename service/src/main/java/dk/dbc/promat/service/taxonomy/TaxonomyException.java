package dk.dbc.promat.service.taxonomy;

import jakarta.ws.rs.core.Response;

public class TaxonomyException extends Exception {
    private int statusCode;

    public TaxonomyException(String err, Response response) {
        super(err);
        this.statusCode = response.getStatus();
    }

    public  TaxonomyException(String err) {
        super(err);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
