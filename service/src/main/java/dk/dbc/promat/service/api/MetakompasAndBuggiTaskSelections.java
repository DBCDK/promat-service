package dk.dbc.promat.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.promat.service.cataloging.CatalogingMarcMapper;
import dk.dbc.promat.service.connectors.CatalogingUpdateConnector;
import dk.dbc.promat.service.connectors.CatalogingUpdateConnectorException;
import dk.dbc.promat.service.dto.BuggiSelectionRequest;
import dk.dbc.promat.service.dto.MetakompasSelectionRequest;
import dk.dbc.promat.service.dto.ServiceErrorCode;
import dk.dbc.promat.service.dto.TagList;
import dk.dbc.promat.service.persistence.JsonMapperProvider;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatEntityManager;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import dk.dbc.promat.service.taskdata.BuggiSelectionEntry;
import dk.dbc.promat.service.taskdata.MetakompasTaskData;
import dk.dbc.promat.service.taxonomy.TaxonomyCache;
import dk.dbc.promat.service.taxonomy.dto.Subject;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateless
public class MetakompasAndBuggiTaskSelections {
    private static final ObjectMapper OBJECT_MAPPER = new JsonMapperProvider().getObjectMapper();
    private final CatalogingMarcMapper marcMapper = new CatalogingMarcMapper();

    @Inject
    TaxonomyCache taxonomyCache;

    @Inject
    @PromatEntityManager
    EntityManager entityManager;

    @Inject
    CatalogingUpdateConnector catalogingUpdateConnector;

    @ConfigProperty(name = "UPDATE_RECORD_LIBRARY_ID", defaultValue = "870970")
    String updateRecordLibraryId;

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

    // READING_EXPERIENCE_ADULT/READING_EXPERIENCE_CHILD tasks store their selection via the dedicated tasks/{taskId}/
    // reading-experience/adult|child endpoints, which validate against the taxonomy - a generic write must not
    // be allowed to bypass that. Clearing the field is the one exception: it needs no validation,
    // and is the established idiom (used by other task types too) for resetting a task's data.
    public boolean isDirectDataWriteAllowed(TaskFieldType type, String data) {
        if(type != TaskFieldType.READING_EXPERIENCE_ADULT && type != TaskFieldType.READING_EXPERIENCE_CHILD) {
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
        // Duplicate options are removed silently: the last value for an option wins, at the option's first position
        Map<Integer, BuggiSelectionEntry> entriesById = new LinkedHashMap<>();
        for(BuggiSelectionRequest request : requests == null ? List.<BuggiSelectionRequest>of() : requests) {
            BuggiSelectionEntry entry = BuggiVocabulary.resolve(request);
            entriesById.put(entry.id(), entry);
        }
        List<BuggiSelectionEntry> entries = new ArrayList<>(entriesById.values());
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
        List<MetakompasTaskData.Entry> entries = new ArrayList<>();
        // Duplicate subjects and suggestions are removed silently, keeping the first occurrence
        Set<MetakompasTaskData.Suggestion> suggestions = new LinkedHashSet<>();
        if(request != null) {
            for(Integer id : request.getIds() == null ? Set.<Integer>of() : new LinkedHashSet<>(request.getIds())) {
                Subject subject = resolveMetakompasSubject(id);
                // Fragile due to path and note both being List<String>: the record's positional
                // constructor gives the compiler no way to catch the two being swapped here.
                entries.add(new MetakompasTaskData.Entry(subject.getPath(), subject.getId(), subject.getTitle(),
                        subject.getNote(), subject.isOftenUsed(), subject.getRef()));
            }
            // A suggestion that repeats a selected subject in the same category is dropped too. Compared
            // exactly - capitalisation can matter, e.g. a proper noun and a common noun
            Set<MetakompasTaskData.Suggestion> selectedAsSuggestions = new HashSet<>();
            for(MetakompasTaskData.Entry entry : entries) {
                selectedAsSuggestions.add(new MetakompasTaskData.Suggestion(entry.path(), entry.title()));
            }
            for(MetakompasTaskData.Suggestion suggestion : request.getSuggestions() == null ? List.<MetakompasTaskData.Suggestion>of() : request.getSuggestions()) {
                if(suggestion != null && suggestion.title() != null && !suggestion.title().isEmpty()) {
                    validateMetakompasPath(suggestion.path());
                    if(!selectedAsSuggestions.contains(suggestion)) {
                        suggestions.add(suggestion);
                    }
                }
            }
        }
        // Same object is both what gets persisted and what is returned to the client - see
        // MetakompasTaskData.Entry/Suggestion for why the two roles don't need separate shapes.
        MetakompasTaskData taskData = new MetakompasTaskData()
                .withEntries(entries)
                .withSuggestions(new ArrayList<>(suggestions));
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

    public void approveReadingExperience(Integer taskId) throws ServiceErrorException {
        PromatTask task = entityManager.find(PromatTask.class, taskId);
        if(task == null) {
            throw new ServiceErrorException(String.format("No task with id %d exists", taskId))
                    .withHttpStatus(404)
                    .withCode(ServiceErrorCode.NOT_FOUND)
                    .withCause("No such task");
        }
        if(task.getTaskFieldType() != TaskFieldType.READING_EXPERIENCE_ADULT && task.getTaskFieldType() != TaskFieldType.READING_EXPERIENCE_CHILD) {
            throw new ServiceErrorException(String.format("Task %d is not a Metakompas or Buggi task", taskId))
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Wrong task type");
        }
        validateNonEmptySelection(task);

        PromatCase promatCase = getCaseOfTask(task.getId());
        try {
            for(String faust : targetFausts(promatCase, task)) {
                // Metakompasset also sends compact MarcXchange update records to
                // update-service; the mapper builds that same kind of registration payload.
                String marcRecord = toMarc(task, faust);
                catalogingUpdateConnector.updateRecord(task.getTaskFieldType(), updateRecordLibraryId + ":" + faust, marcRecord);
            }
        } catch(CatalogingUpdateConnectorException e) {
            throw new ServiceErrorException("Failed to register reading-experience selection in update-service")
                    .withHttpStatus(502)
                    .withCode(ServiceErrorCode.FAILED)
                    .withCause("update-service registration failed")
                    .withDetails(e.getMessage());
        } catch(JsonProcessingException e) {
            throw new ServiceErrorException("Failed to parse persisted task selection")
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid task data")
                    .withDetails(e.getMessage());
        }
        task.setApproved(LocalDate.now());
    }

    private PromatCase getCaseOfTask(int taskId) {
        TypedQuery<PromatCase> query = entityManager.createNamedQuery(
                PromatCase.GET_CASE_WITH_TASK_ID_NAME, PromatCase.class);
        query.setParameter("taskid", taskId);
        return query.getSingleResult();
    }

    private List<String> targetFausts(PromatCase promatCase, PromatTask task) {
        // Most tasks update the primary faust, but task-specific target fausts are
        // supported when the case data points registration at one or more records.
        if(task.getTargetFausts() == null || task.getTargetFausts().isEmpty()) {
            return List.of(promatCase.getPrimaryFaust());
        }
        return task.getTargetFausts();
    }

    private String toMarc(PromatTask task, String faust) throws JsonProcessingException {
        MetakompasTaskData metakompasSelectionData = null;
        List<BuggiSelectionEntry> buggiEntries = null;
        if(task.getTaskFieldType() == TaskFieldType.READING_EXPERIENCE_ADULT) {
            metakompasSelectionData = OBJECT_MAPPER.readValue(task.getData(), MetakompasTaskData.class);
        } else if(task.getTaskFieldType() == TaskFieldType.READING_EXPERIENCE_CHILD) {
            buggiEntries = OBJECT_MAPPER.readValue(task.getData(), new TypeReference<>() {});
        }
        return marcMapper.toMarc(task.getTaskFieldType(), updateRecordLibraryId, faust, metakompasSelectionData, buggiEntries);
    }

    private void validateNonEmptySelection(PromatTask task) throws ServiceErrorException {
        if(task.getData() == null || task.getData().isBlank()) {
            throw emptySelection(task);
        }
        try {
            // Approval is the final gate before registration, so do not enqueue tasks
            // where the saved JSON cannot produce a meaningful update-service record.
            if(task.getTaskFieldType() == TaskFieldType.READING_EXPERIENCE_ADULT) {
                MetakompasTaskData data = OBJECT_MAPPER.readValue(task.getData(), MetakompasTaskData.class);
                boolean hasEntries = data.getEntries() != null && !data.getEntries().isEmpty();
                boolean hasSuggestions = data.getSuggestions() != null && !data.getSuggestions().isEmpty();
                if(!hasEntries && !hasSuggestions) {
                    throw emptySelection(task);
                }
            } else if(task.getTaskFieldType() == TaskFieldType.READING_EXPERIENCE_CHILD) {
                List<BuggiSelectionEntry> buggiEntries = OBJECT_MAPPER.readValue(task.getData(), new TypeReference<>() {});
                if(buggiEntries.isEmpty()) {
                    throw emptySelection(task);
                }
            }
        } catch(JsonProcessingException e) {
            throw new ServiceErrorException("Failed to parse persisted task selection")
                    .withHttpStatus(400)
                    .withCode(ServiceErrorCode.INVALID_REQUEST)
                    .withCause("Invalid task data")
                    .withDetails(e.getMessage());
        }
    }

    private ServiceErrorException emptySelection(PromatTask task) {
        return new ServiceErrorException(String.format("Task %d has no selection to approve", task.getId()))
                .withHttpStatus(400)
                .withCode(ServiceErrorCode.INVALID_REQUEST)
                .withCause("Empty selection");
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
