/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.PromatUser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Formatting {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M yyyy");
    public static String format(LocalDate localDate) {
        return localDate.format(formatter);
    }

    public static <T extends PromatUser> String format(T user) {
        var names = new ArrayList<String>();

        if( user.getFirstName() != null ) {
            names.addAll(Arrays.asList(user.getFirstName().split(" ")));
        }
        if( user.getLastName() != null ) {
            names.addAll(Arrays.asList(user.getLastName().split(" ")));
        }

        return names.size() > 0 ? names.stream().collect(Collectors.joining(" ")) : "";
    }
}
