package dk.dbc.promat.service.util;

import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PromatTaskUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PromatTaskUtils.class);

    public Optional<PromatTask> getTaskForMainFaust(PromatCase promatCase, TaskFieldType taskFieldType) {
        return promatCase.getTasks()
                .stream()
                .filter(promatTask ->
                        promatTask.getTaskFieldType() == taskFieldType &&
                                (promatTask.getTargetFausts() == null
                                        || promatTask.getTargetFausts().isEmpty())).findFirst();


    }

    public Optional<PromatTask> getTaskForRelatedFaust(PromatCase promatCase, TaskFieldType taskFieldType) {
        return promatCase.getTasks()
                .stream()
                .filter(promatTask -> promatTask.getTaskFieldType() == taskFieldType &&
                        (promatTask.getTargetFausts() != null )).findFirst();
    }

}
