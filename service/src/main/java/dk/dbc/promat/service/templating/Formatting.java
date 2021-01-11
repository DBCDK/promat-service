/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Formatting {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M yyyy");
    public static String format(LocalDate localDate) {
        return localDate.format(formatter);
    }
}
