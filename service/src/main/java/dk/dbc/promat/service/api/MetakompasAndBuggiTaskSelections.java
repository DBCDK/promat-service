package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.Tag;
import dk.dbc.promat.service.dto.TagList;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasSuggestion;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

@Stateless
public class MetakompasAndBuggiTaskSelections {
    private static final ObjectMapper OBJECT_MAPPER = new JsonMapperProvider().getObjectMapper();
    @Inject
    TaxonomyCache taxonomyCache;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;
    // Resolved against the current taxonomy tree at write time only - a saved selection may
    // later reference an id that no longer resolves; that's expected, not an error.
    public Subject resolveMetakompasSubject(Integer id) throws ServiceErrorException {
        if(taxonomyCache.get().isEmpty()) {
            // Distinguished from a genuinely-unresolvable id below - this means the taxonomy
            // hasn't synced yet (cold pod) or Kafka isn't configured at all, not that the id is
            // wrong, and a client needs to be able to tell the two apart.
            throw new ServiceErrorException("Taxonomy is not yet populated - unable to resolve any metakompas subject")
                    .withHttpStatus(503)
                    .withCode(ServiceErrorCode.FAILED)
                    .withCause("Taxonomy cache is empty");
        }
        Subject subject = id == null ? null : taxonomyCache.get().getById(id);
        if(subject == null) {
            throw new ServiceErrorException(String.format("Metakompas subject id %s does not exist in the taxonomy", id))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid metakompas subject");
        }
        return subject;
    }

    // METAKOMPAS/BUGGI tasks store their selection via the dedicated tasks/{taskId}/
    // metakompas|buggi endpoints, which validate against the taxonomy - a generic write must not
    // be allowed to bypass that. Clearing the field is the one exception: it needs no validation,
    // and is the established idiom (used by other task types too) for resetting a task's data.
    public boolean isDirectDataWriteAllowed(TaskFieldType type, String data) {
        if(type != TaskFieldType.METAKOMPAS && type != TaskFieldType.BUGGI) {
            return true;
        }
        return data == null || data.isEmpty();
    }

    // Selections are stored directly on PromatTask.data, shared across all of the task's
    // target fausts.
    public PromatTask resolveTaskForSelection(Integer taskId, TaskFieldType expectedType) throws ServiceErrorException {
        PromatTask task = entityManager.find(PromatTask.class, taskId);
        if(task == null) {
            throw new ServiceErrorException(String.format("No task with id %d exists", taskId))
                    .withHttpStatus(404)
                    .withCode(ServiceErrorCode.NOT_FOUND)
                    .withCause("No such task");
        }
        if(task.getTaskFieldType() != expectedType) {
            throw new ServiceErrorException(String.format("Task %d is not a %s task", taskId, expectedType))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Wrong task type");
        }
        return task;
    }

    public TagList writeBuggiSelection(PromatTask task, TagList tags) throws ServiceErrorException {
        try {
            task.setData(OBJECT_MAPPER.writeValueAsString(tags));
        } catch(JsonProcessingException e) {
            throw new ServiceErrorException("Failed to serialize buggi selection")
                    .withHttpStatus(500)
                    .withCode(ServiceErrorCode.FAILED)
                    .withDetails(e.getMessage());
        }
        return tags;
    }

    public List<BuggiSelectionEntry> writeBuggiSelection(PromatTask task, List<BuggiSelectionRequest> requests) throws ServiceErrorException {
        List<BuggiSelectionEntry> entries = new ArrayList<>();
        for(BuggiSelectionRequest request : requests == null ? List.<BuggiSelectionRequest>of() : requests) {
            entries.add(BuggiVocabulary.resolve(request));
        }
        try {
            task.setData(OBJECT_MAPPER.writeValueAsString(entries));
        } catch(JsonProcessingException e) {
            throw new ServiceErrorException("Failed to serialize buggi selection")
                    .withHttpStatus(500)
                    .withCode(ServiceErrorCode.FAILED)
                    .withDetails(e.getMessage());
        }
        return entries;
    }


    public MetakompasTaskData writeMetakompasSelection(PromatTask task, MetakompasSelectionRequest request) throws ServiceErrorException {
        List<MetakompasSelectionEntry> entries = new ArrayList<>();
        List<MetakompasSuggestion> suggestions = new ArrayList<>();
        if(request != null) {
            for(Integer id : request.getIds() == null ? List.<Integer>of() : request.getIds()) {
                Subject subject = resolveMetakompasSubject(id);
                // Fragile due to path and note both being List<String>: the record's positional
                // constructor gives the compiler no way to catch the two being swapped here.
                entries.add(new MetakompasSelectionEntry(subject.getPath(), subject.getId(), subject.getTitle(),
                        subject.getNote(), subject.isOftenUsed(), subject.getRef()));
            }
            for(MetakompasSuggestion suggestion : request.getSuggestions() == null ? List.<MetakompasSuggestion>of() : request.getSuggestions()) {
                if(suggestion != null && suggestion.text() != null && !suggestion.text().isEmpty()) {
                    validateMetakompasPath(suggestion.path());
                    suggestions.add(suggestion);
                }
            }
        }
        // Same object is both what gets persisted and what is returned to the client - see
        // MetakompasSelectionEntry/MetakompasSuggestion for why the two roles don't need
        // separate shapes here.
        MetakompasTaskData taskData = new MetakompasTaskData()
                .withEntries(entries)
                .withSuggestions(suggestions);
        try {
            task.setData(OBJECT_MAPPER.writeValueAsString(taskData));
        } catch(JsonProcessingException e) {
            throw new ServiceErrorException("Failed to serialize metakompas selection")
                    .withHttpStatus(500)
                    .withCode(ServiceErrorCode.FAILED)
                    .withDetails(e.getMessage());
        }
        return taskData;
    }

    private void validateMetakompasPath(List<String> path) throws ServiceErrorException {
        if(!taxonomyCache.get().hasPath(path)) {
            throw new ServiceErrorException(String.format("Metakompas path %s does not exist in the taxonomy", path))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid metakompas path");
        }
    }

}
