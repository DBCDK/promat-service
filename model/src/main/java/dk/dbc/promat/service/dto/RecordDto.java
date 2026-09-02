package dk.dbc.promat.service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// A DTO ("Data Transfer Object") is a plain class whose only job is to
// carry data across a boundary - here, from the backend to whatever calls
// GET /v1/api/records/{id} or /records/search - as JSON. It's deliberately
// dumb: no business logic, just fields plus the boilerplate below them.
// Everything in this file is repetitive by design:
//  - getX()/setX() : the classic Java "bean" pattern many libraries
//    (including the JSON library used here) expect.
//  - withX(...) returning `this` : a "fluent builder" style, used instead
//    of a giant constructor - see RecordsProvider for how it's used, e.g.
//    `new RecordDto().withFaust(...).withTitle(...)`.
//  - equals()/hashCode()/toString() : written by hand here field-by-field;
//    a Java `record` (like FbiApiHandler.RecordInfo) gets these for free,
//    but this project's DTOs predate that being the norm for this class.
public class RecordDto implements Dto {

    private String faust;
    // Named "isPrimary" rather than "primary" so the generated getter below
    // reads naturally as a yes/no question: isPrimary(). This is the
    // manifestation that matches the id the caller originally asked for -
    // see RecordsProvider.getRecords().
    private boolean isPrimary;
    // General + specific material type (e.g. general "BOOK", specific
    // "audiobook"). A list because a manifestation can technically have more
    // than one, though in practice this project only ever fills in one.
    private List<RecordMaterialTypeDto> types = new ArrayList<>();
    private String title;
    private String creator;
    private String publisher;
    private String extent;
    private String edition;
    private List<String> isbn = new ArrayList<>();
    private List<String> dk5 = new ArrayList<>();
    private List<String> series = new ArrayList<>();
    private List<String> targetGroup = new ArrayList<>();
    private List<String> catalogCodes = new ArrayList<>();

    public String getFaust() {
        return faust;
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getExtent() {
        return extent;
    }

    public void setExtent(String extent) {
        this.extent = extent;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public List<String> getIsbn() {
        return isbn;
    }

    public void setIsbn(List<String> isbn) {
        this.isbn = isbn;
    }

    public List<String> getDk5() {
        return dk5;
    }

    public void setDk5(List<String> dk5) {
        this.dk5 = dk5;
    }

    public List<String> getSeries() {
        return series;
    }

    public void setSeries(List<String> series) {
        this.series = series;
    }

    public List<String> getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(List<String> targetGroup) {
        this.targetGroup = targetGroup;
    }

    public List<String> getCatalogCodes() {
        return catalogCodes;
    }

    public void setCatalogCodes(List<String> catalogCodes) {
        this.catalogCodes = catalogCodes;
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

    public RecordDto withTitle(String title) {
        this.title = title;
        return this;
    }

    public RecordDto withCreator(String creator) {
        this.creator = creator;
        return this;
    }

    public RecordDto withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public RecordDto withExtent(String extent) {
        this.extent = extent;
        return this;
    }

    public RecordDto withEdition(String edition) {
        this.edition = edition;
        return this;
    }

    public RecordDto withIsbn(List<String> isbn) {
        this.isbn = isbn;
        return this;
    }

    public RecordDto withDk5(List<String> dk5) {
        this.dk5 = dk5;
        return this;
    }

    public RecordDto withSeries(List<String> series) {
        this.series = series;
        return this;
    }

    public RecordDto withTargetGroup(List<String> targetGroup) {
        this.targetGroup = targetGroup;
        return this;
    }

    public RecordDto withCatalogCodes(List<String> catalogCodes) {
        this.catalogCodes = catalogCodes;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        RecordDto recordDto = (RecordDto) o;
        return isPrimary == recordDto.isPrimary &&
                faust.equals(recordDto.faust) &&
                types.equals(recordDto.types) &&
                Objects.equals(title, recordDto.title) &&
                Objects.equals(creator, recordDto.creator) &&
                Objects.equals(publisher, recordDto.publisher) &&
                Objects.equals(extent, recordDto.extent) &&
                Objects.equals(edition, recordDto.edition) &&
                Objects.equals(isbn, recordDto.isbn) &&
                Objects.equals(dk5, recordDto.dk5) &&
                Objects.equals(series, recordDto.series) &&
                Objects.equals(targetGroup, recordDto.targetGroup) &&
                Objects.equals(catalogCodes, recordDto.catalogCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faust, isPrimary, types, title, creator, publisher, extent, edition, isbn, dk5, series, targetGroup, catalogCodes);
    }

    @Override
    public String toString() {
        return "RecordDto{" +
                "faust='" + faust + '\'' +
                ", isPrimary=" + isPrimary +
                ", types=" + types +
                ", title='" + title + '\'' +
                ", creator='" + creator + '\'' +
                ", publisher='" + publisher + '\'' +
                ", extent='" + extent + '\'' +
                ", edition='" + edition + '\'' +
                ", isbn=" + isbn +
                ", dk5=" + dk5 +
                ", series=" + series +
                ", targetGroup=" + targetGroup +
                ", catalogCodes=" + catalogCodes +
                '}';
    }
}
