/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service;

import dk.dbc.opennumberroll.OpennumberRollConnector;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.api.ServiceErrorException;
import dk.dbc.promat.service.dto.ServiceErrorCode;
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
     *
     * @param entityClass class of the entity
     * @param primaryKey  the primary key of the entity
     * @param <T>         the type
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
                        .withDetails("Field 'subject' contains id " + subjectId + " which does not exist");
            }
            subjects.add(subject);
        }
        return subjects;
    }

    public List<SubjectNote> resolveSubjectNotes(List<Integer> subjects, Collection<SubjectNote> subjectNotes) {
        if (subjectNotes == null || subjects == null) {
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
        return taskFieldType.getPaymentCategory(taskType);
    }

    public static PayCategory getPayCategoryForTaskType(TaskType taskType) {
        return taskType.payCategory;
    }

    public void assignFaustnumber(PromatCase existing) throws OpennumberRollConnectorException {
        List<PromatTask> tasks = existing.getTasks().stream()
                .filter(task -> task.getTaskFieldType() == TaskFieldType.BRIEF)
                .filter(task -> task.getRecordId() == null || task.getRecordId().isEmpty())
                .collect(Collectors.toList());
        for (PromatTask task : tasks) {
            OpennumberRollConnector.Params params = new OpennumberRollConnector.Params().withRollName(openNumberrollRollName);
            task.setRecordId(opennumberRollConnector.getId(params));
            LOGGER.info("Assigned new faustnumber {} to task with id {} on case with id {}", task.getRecordId(), task.getRecordId(), existing.getId());
        }
    }
}
