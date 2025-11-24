package dk.dbc.promat.service.api;

import dk.dbc.promat.service.dto.ServiceErrorCode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class Faustnumbers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Faustnumbers.class);

    static boolean checkNoOpenCaseWithFaust(EntityManager entityManager, String... fausts) {
        return checkNoOpenCaseWithFaust(entityManager, null, fausts);
    }

    static boolean checkNoOpenCaseWithFaust(EntityManager entityManager, Integer caseId, String... fausts) {
        Query q = entityManager.createNativeQuery("SELECT CheckNoOpenCaseWithFaust(?, ?)");

        // Passing a null parameter is not possible, so if caseId is null, then passing
        // the id of a non-existing case will yield same results (and id zero never exists)
        q.setParameter(2, caseId != null ? caseId : 0);

        for (String faust : fausts) {
            q.setParameter(1, faust);
            if ((boolean) q.getSingleResult() == false) {
                return false;
            }
        }

        return true;
    }

    public static <T> void checkForNullFausts(List<String> fausts, T task) throws ServiceErrorException {
        if (fausts != null && !fausts.isEmpty()) {
            if (fausts.contains(null) || fausts.contains("")) {
                throw new ServiceErrorException("Invalid target faustnumber")
                        .withCause("Invalid target faustnumber")
                        .withDetails("One or more target faustnumbers are null or empty. Task: " + task)
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withHttpStatus(400);
            }
        }
    }


}
