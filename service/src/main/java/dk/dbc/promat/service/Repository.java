/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.dto.ServiceErrorCode;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import dk.dbc.opennumberroll.OpennumberRollConnector;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.persistence.PayCategory;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.Subject;
import dk.dbc.promat.service.persistence.SubjectNote;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.persistence.TaskType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    OpennumberRollConnector opennumberRollConnector;

    @Inject
    @ConfigProperty(name = "OPENNUMBERROLL_NUMBERROLLNAME")
    String openNumberrollRollName;

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

    public List<SubjectNote> resolveSubjectNotes(List<Integer> subjects, Collection<SubjectNote> subjectNotes) {
        if(subjectNotes == null || subjects == null) {
            return List.of();
        }
        return subjectNotes
                .stream()
                .filter(subjectNote -> subjects.contains(subjectNote.getSubjectId()))
                .collect(Collectors.toList());
    }

    public List<SubjectNote> checkSubjectNotes(List<SubjectNote> subjectNotes, List<Integer> reviewerSubjectIds) throws ServiceErrorException {
        if (subjectNotes == null || subjectNotes.isEmpty()) {
            return Collections.emptyList();
        }

        for (SubjectNote subjectNote : subjectNotes) {
            if (!reviewerSubjectIds.contains(subjectNote.getSubjectId())) {
                throw new ServiceErrorException("Attempt to resolve subjectNote in reviewers subjects failed")
                        .withHttpStatus(400)
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("No such subject")
                        .withDetails(String.format("Field 'subjects' values '%s' has no subject %s", reviewerSubjectIds, subjectNote.getSubjectId()));
            }
        }
        return subjectNotes;
    }

    public static PayCategory getPayCategoryForTaskFieldTypeOfTaskType(TaskType taskType, TaskFieldType taskFieldType) throws ServiceErrorException {
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
                return getPayCategoryForTaskType(taskType);
            }
        }
    }

    public static PayCategory getPayCategoryForTaskType(TaskType taskType) throws ServiceErrorException {
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

            case NONE:
                return PayCategory.NONE;
        }

        throw new ServiceErrorException("Bad PayCategory")
                .withDetails(String.format("Invalid TaskType %s when determining paycategory for TaskType", taskType))
                .withCode(ServiceErrorCode.INVALID_REQUEST)
                .withHttpStatus(400);
    }

    public void assignFaustnumber(PromatCase existing) throws OpennumberRollConnectorException {
        for(PromatTask task : existing.getTasks().stream()
                .filter(task -> task.getTaskFieldType() == TaskFieldType.BRIEF)
                .collect(Collectors.toList())) {
            if( task.getRecordId() == null || task.getRecordId().isEmpty() ) {
                OpennumberRollConnector.Params params = new OpennumberRollConnector.Params();
                params.withRollName(openNumberrollRollName);
                task.setRecordId(opennumberRollConnector.getId(params));
                LOGGER.info("Assigned new faustnumber {} to task with id {} on case with id {}", task.getRecordId(), task.getRecordId(), existing.getId());
            }
        }
    }
}
