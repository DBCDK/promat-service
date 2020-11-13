package dk.dbc.promat.service.templating;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Formatting {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M yyyy");
    public static String format(LocalDate localDate) {
        return localDate.format(formatter);
    }
}
