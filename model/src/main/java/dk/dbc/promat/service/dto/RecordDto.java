/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecordDto implements Dto {

    private String faust;
    private boolean isPrimary;
    private List<RecordMaterialTypeDto> types = new ArrayList<>();

    public String getFaust() {
        return faust;
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public void setTypes(List<RecordMaterialTypeDto> types) {
        this.types = types;
    }

    public List<RecordMaterialTypeDto> getTypes() {
        return types;
    }

    public RecordDto withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public RecordDto withPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
        return this;
    }

    public RecordDto withTypes(List<RecordMaterialTypeDto> types) {
        this.types = types;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        RecordDto recordDto = (RecordDto) o;
        return isPrimary == recordDto.isPrimary &&
                faust.equals(recordDto.faust) &&
                types.equals(recordDto.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faust, isPrimary, types);
    }

    @Override
    public String toString() {
        return "RecordDto{" +
                "faust='" + faust + '\'' +
                ", isPrimary=" + isPrimary +
                ", types=" + types +
                '}';
    }
}
