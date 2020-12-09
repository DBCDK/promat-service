/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.persistence;

import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.dto.ServiceErrorCode;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Stateless
public class Repository {
    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    /**
     * Locks a single entity for exclusive access.
     * This is a blocking operation.
     * @param entityClass class of the entity
     * @param primaryKey the primary key of the entity
     * @param <T> the type
     * @return locked entity
     */
    public <T> T getExclusiveAccessToEntity(Class<T> entityClass, Object primaryKey) {
        return entityManager.find(entityClass, primaryKey, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Locks a table for exclusive access
     * This is a blocking operation.
     * @param tableName name of the table
     */
    public void getExclusiveAccessToTable(String tableName) {
        final Query lockQuery = entityManager.createNativeQuery(
                String.format("LOCK TABLE %s IN EXCLUSIVE MODE", tableName)
        );
        lockQuery.executeUpdate();
    }

    public List<Subject> resolveSubjects(List<Integer> subjectIds) throws ServiceErrorException {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Subject> subjects = new ArrayList<>();
        for (int subjectId : subjectIds) {
            final Subject subject = entityManager.find(Subject.class, subjectId);
            if (subject == null) {
                throw new ServiceErrorException("Attempt to resolve subject failed")
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("No such subject")
                        .withDetails(String.format("Field 'subject' contains id {} which does not exist", subjectId));
            }
            subjects.add(subject);
        }
        return subjects;
    }
}
