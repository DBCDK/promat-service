package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CaseViewJsonExtract(
        String title,
        String author,
        String publisher,
        String faust,
        String reviewer,
        String editor,
        String deadline,
        String status,
        String BKM,
        Map<String, String> briefs,
        String description,
        String evaluation,
        String comparison,
        String recommendation,
        Map<String, String> topics,
        String age,
        String level
) {
    public static CaseViewJsonExtract from(List<String> relatedFausts, PromatCase promatCase) {
        return new CaseViewJsonExtract(
                promatCase.getTitle(),
                promatCase.getAuthor(),
                promatCase.getPublisher(),
                fausts(promatCase.getPrimaryFaust(), relatedFausts),
                Formatting.format(promatCase.getReviewer()),
                Formatting.format(promatCase.getEditor()),
                Formatting.format(promatCase.getDeadline()),
                Formatting.format(promatCase.getStatus()),
                taskData(promatCase, TaskFieldType.BKM),
                tasksMap(promatCase, TaskFieldType.BRIEF),
                taskData(promatCase, TaskFieldType.DESCRIPTION),
                taskData(promatCase, TaskFieldType.EVALUATION),
                taskData(promatCase, TaskFieldType.COMPARISON),
                taskData(promatCase, TaskFieldType.RECOMMENDATION),
                tasksMap(promatCase, TaskFieldType.TOPICS),
                taskData(promatCase, TaskFieldType.AGE),
                taskData(promatCase, TaskFieldType.MATLEVEL)
        );
    }

    private static String taskData(PromatCase promatCase, TaskFieldType type) {
        return promatCase.getTasks().stream()
                .filter(t -> t.getTaskFieldType() == type)
                .map(PromatTask::getData)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, String> tasksMap(PromatCase promatCase, TaskFieldType type) {
        return promatCase.getTasks().stream()
                .filter(t -> t.getTaskFieldType() == type)
                .filter(t -> t.getTargetFausts() != null && t.getData() != null)
                .collect(Collectors.toMap(
                        t -> String.join(", ", t.getTargetFausts()),
                        PromatTask::getData,
                        (a, b) -> a
                ));
    }

    private static String fausts(String primaryFaust, List<String> relatedFausts) {
        if (relatedFausts == null || relatedFausts.isEmpty()) {
            return primaryFaust;
        }
        return primaryFaust + " + (" + String.join(", ", relatedFausts) + ")";
    }
}
