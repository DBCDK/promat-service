package dk.dbc.promat.service.dto;

import java.util.Objects;

public class RecordDto {

    private String faust;
    private boolean isPrimary;

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

    public RecordDto withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public RecordDto withPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        RecordDto recordDto = (RecordDto) o;
        return isPrimary == recordDto.isPrimary &&
                faust.equals(recordDto.faust);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faust, isPrimary);
    }
}
