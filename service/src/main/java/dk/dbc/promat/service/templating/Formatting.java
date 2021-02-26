/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.persistence.CaseStatus;
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

        if (user == null) {
            return "";
        }
        if( user.getFirstName() != null ) {
            names.addAll(Arrays.asList(user.getFirstName().split(" ")));
        }
        if( user.getLastName() != null ) {
            names.addAll(Arrays.asList(user.getLastName().split(" ")));
        }

        return names.size() > 0 ? names.stream().collect(Collectors.joining(" ")) : "";
    }

    public static String format(CaseStatus status) {
        switch(status) {
            case CREATED:
                return "Oprettet";
            case ASSIGNED:
                return "Tildelt";
            case REJECTED:
                return "Afvist";
            case PENDING_APPROVAL:
                return "Afventer godkendelse";
            case PENDING_ISSUES:
                return "Afventer rettelser";
            case PENDING_EXTERNAL:
                return "Afventer eksterne opgaver";
            case APPROVED:
                return "Godkendt";
            case PENDING_MEETING:
                return "Klar til gennemsyn";
            case PENDING_EXPORT:
                return "Klar til publicering";
            case EXPORTED:
                return "Publiceret";
            case PENDING_REVERT:
                return "Afventer tilbagetrækning";
            case REVERTED:
                return "Trukket tilbage";
            case PENDING_CLOSE:
                return "Afventer lukning";
            case CLOSED:
                return "Lukket";
            case DELETED:
                return "Slettet";
            default:
                return "";
        }
    }
}
