/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;

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
                        .withHttpStatus(400)
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("No such subject")
                        .withDetails(String.format("Field 'subject' contains id {} which does not exist", subjectId));
            }
            subjects.add(subject);
        }
        return subjects;
    }

    public static PayCategory getPayCategoryForTaskType(TaskType taskType, TaskFieldType taskFieldType) throws ServiceErrorException {
        switch(taskFieldType) {
            case BRIEF:
                return PayCategory.BRIEF;
            case METAKOMPAS:
                return PayCategory.METAKOMPAS;
            case BKM:
                return PayCategory.BKM;
            case EXPRESS:
                return PayCategory.EXPRESS;
            default: {
                switch(taskType) {

                    case GROUP_1_LESS_THAN_100_PAGES:
                        return PayCategory.GROUP_1_LESS_THAN_100_PAGES;
                    case GROUP_2_100_UPTO_199_PAGES:
                        return PayCategory.GROUP_2_100_UPTO_199_PAGES;
                    case GROUP_3_200_UPTO_499_PAGES:
                        return PayCategory.GROUP_3_200_UPTO_499_PAGES;
                    case GROUP_4_500_OR_MORE_PAGES:
                        return PayCategory.GROUP_4_500_OR_MORE_PAGES;

                    case MOVIES_GR_1:
                        return PayCategory.MOVIES_GR_1;
                    case MOVIES_GR_2:
                        return PayCategory.MOVIES_GR_2;
                    case MOVIES_GR_3:
                        return PayCategory.MOVIES_GR_3;

                    case MULTIMEDIA_FEE:
                        return PayCategory.MULTIMEDIA_FEE;
                    case MULTIMEDIA_FEE_GR2:
                        return PayCategory.MULTIMEDIA_FEE_GR2;

                    case MOVIE_NON_FICTION_GR1:
                        return PayCategory.MOVIE_NON_FICTION_GR1;
                    case MOVIE_NON_FICTION_GR2:
                        return PayCategory.MOVIE_NON_FICTION_GR2;
                    case MOVIE_NON_FICTION_GR3:
                        return PayCategory.MOVIE_NON_FICTION_GR3;
                }
            }
        }

        throw new ServiceErrorException("Bad PayCategory")
                .withDetails(String.format("Invalid combination of TaskType %s and TaskFieldType %s when determining paycategory", taskType, taskFieldType))
                .withCode(ServiceErrorCode.INVALID_REQUEST)
                .withHttpStatus(400);
    }
}
