package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.dto.MetakompasSelectionData;
import dk.dbc.promat.service.dto.MetakompasSelectionEntry;
import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionResult;
import dk.dbc.promat.service.dto.MetakompasSelectionView;
import dk.dbc.promat.service.dto.MetakompasSuggestion;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.Tag;
import dk.dbc.promat.service.dto.TagList;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

@Stateless
public class TaskSelections {
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

    public List<Tag> writeBuggiSelection(PromatTask task, List<Tag> tags) throws ServiceErrorException {
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


    public MetakompasSelectionResult writeMetakompasSelection(PromatTask task, List<MetakompasSelectionRequest> requests) throws ServiceErrorException {
        List<MetakompasSelectionEntry> entries = new ArrayList<>();
        List<MetakompasSelectionView> views = new ArrayList<>();
        List<MetakompasSuggestion> suggestions = new ArrayList<>();
        for(MetakompasSelectionRequest request : requests == null ? List.<MetakompasSelectionRequest>of() : requests) {
            if(request == null) {
                throw new ServiceErrorException("Metakompas selection request entry must not be null")
                        .withHttpStatus(400)
                        .withCode(ServiceErrorCode.INVALID_REQUEST)
                        .withCause("Invalid metakompas path");
            }
            validateMetakompasPath(request.getPath());
            for(Integer id : request.getIds() == null ? List.<Integer>of() : request.getIds()) {
                Subject subject = resolveMetakompasSubject(id);
                MetakompasSelectionEntry entry = new MetakompasSelectionEntry()
                        .withPath(subject.getPath())
                        .withId(subject.getId())
                        .withTitle(subject.getTitle())
                        .withNote(subject.getNote())
                        .withOftenUsed(subject.isOftenUsed())
                        .withRef(subject.getRef());
                entries.add(entry);
                views.add(MetakompasSelectionView.from(entry));
            }
            for(String suggestion : request.getSuggestions() == null ? List.<String>of() : request.getSuggestions()) {
                if(suggestion != null && !suggestion.isEmpty()) {
                    suggestions.add(new MetakompasSuggestion()
                            .withPath(request.getPath())
                            .withText(suggestion));
                }
            }
        }
        try {
            task.setData(OBJECT_MAPPER.writeValueAsString(new MetakompasSelectionData()
                    .withEntries(entries)
                    .withSuggestions(suggestions)));
        } catch(JsonProcessingException e) {
            throw new ServiceErrorException("Failed to serialize metakompas selection")
                    .withHttpStatus(500)
                    .withCode(ServiceErrorCode.FAILED)
                    .withDetails(e.getMessage());
        }
        return new MetakompasSelectionResult()
                .withEntries(views)
                .withSuggestions(suggestions);
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
