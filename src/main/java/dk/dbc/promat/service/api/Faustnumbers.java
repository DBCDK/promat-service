package dk.dbc.promat.service.api;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class Faustnumbers {

    static boolean checkNoOpenCaseWithFaust(EntityManager entityManager, String... fausts) {
        return checkNoOpenCaseWithFaust(entityManager, null, fausts);
    }

    static boolean checkNoOpenCaseWithFaust(EntityManager entityManager, Integer caseId, String... fausts) {
        Query q = entityManager.createNativeQuery("SELECT CheckNoOpenCaseWithFaust(?, ?)");

        // Passing a null parameter is not possible, so if caseId is null, then passing
        // the id of a non-existing case will yield same results (and id zero never exists)
        q.setParameter(2, caseId != null ? caseId : 0);

        for( String faust : fausts) {
            q.setParameter(1, faust);
            if( (boolean) q.getSingleResult() == false ) {
                return false;
            }
        }

        return true;
    }
}
