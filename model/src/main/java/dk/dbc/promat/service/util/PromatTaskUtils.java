package dk.dbc.promat.service.util;

import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromatTaskUtils {

    public static List<PromatTask> getTasksOfType(PromatCase promatCase, TaskFieldType taskFieldType) {
        if (promatCase.getTasks() == null) {
            return Collections.emptyList();
        }

        return promatCase.getTasks()
                .stream()
                .filter(promatTask -> promatTask.getTaskFieldType() == taskFieldType)
                .collect(Collectors.toList());
    }

    public static String getFormattedDataForLinksMarkup(String input) {
        // The input string should have all link markups indicated by a matching set of '<t>...<t>' tags.
        // It would be far better to use proper xhtml-style tags like '<t>...</t>', but I think it will take
        // some convincing to convert all reviewers, so we have to handle the old case

        // Since we have no end tag, we need to iterate the string by <t> tags C-style
        String intermediate = input;
        boolean insideTag = false;
        String output = "";
        while( !intermediate.isEmpty() ) {
            int found = intermediate.indexOf("<t>");
            if( found >= 0 ) {
                output += intermediate.substring(0, found);
                if( !insideTag ) {
                    output += "<span style=\"font-style: italic;\">";
                    insideTag = true;
                } else {
                    output += "</span>";
                    insideTag = false;
                }
                intermediate = intermediate.substring(found + 3);
            } else {
                output += intermediate;
                break;
            }
        }

        return output;
    }
}
