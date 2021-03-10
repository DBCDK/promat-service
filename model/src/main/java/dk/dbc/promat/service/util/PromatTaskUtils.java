package dk.dbc.promat.service.util;

import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromatTaskUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PromatTaskUtils.class);

    public static Optional<PromatTask> getTaskForMainFaust(PromatCase promatCase, TaskFieldType taskFieldType) {
        return promatCase.getTasks()
                .stream()
                .filter(promatTask -> promatTask.getTaskFieldType() == taskFieldType)
                .filter(promatTask -> promatTask.getTargetFausts() == null || promatTask.getTargetFausts().isEmpty() ||
                        promatTask.getTargetFausts().contains(promatCase.getPrimaryFaust())).findFirst();

    }

    public static Optional<PromatTask> getTaskForRelatedFaust(PromatCase promatCase, TaskFieldType taskFieldType) {
        return promatCase.getTasks()
                .stream()
                .filter(promatTask -> promatTask.getTaskFieldType() == taskFieldType)
                .filter(promatTask -> promatTask.getTargetFausts() != null && !promatTask.getTargetFausts().isEmpty())
                .findFirst();
    }

}
