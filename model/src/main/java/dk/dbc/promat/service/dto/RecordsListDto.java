/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import java.util.List;
import java.util.Objects;

public class RecordsListDto implements Dto {

    private int numFound;
    private List<RecordDto> records;

    public int getNumFound() {
        return numFound;
    }

    public void setNumFound(int numFound) {
        this.numFound = numFound;
    }

    public List<RecordDto> getRecords() {
        return records;
    }

    public void setRecords(List<RecordDto> records) {
        this.records = records;
    }

    public RecordsListDto withNumFound(int found) {
        this.numFound = found;
        return this;
    }

    public RecordsListDto withRecords(List<RecordDto> records) {
        this.records = records;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        RecordsListDto that = (RecordsListDto) o;
        return numFound == that.numFound &&
                records.equals(that.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numFound, records);
    }
}
