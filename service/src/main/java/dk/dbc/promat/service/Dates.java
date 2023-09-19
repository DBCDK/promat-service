package dk.dbc.promat.service;

import jakarta.ejb.Stateless;
import java.time.LocalDate;

public class Dates {

    public LocalDate getCurrentDate() {
        return LocalDate.now();
    }
}
