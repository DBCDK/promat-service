/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.api;

import java.util.ArrayList;
import java.util.List;

public class BibliographicInformation {

    private String faust = "";
    private String creator = "";
    private List<String> dk5 = new ArrayList<>();
    private List<String> isbn = new ArrayList<>();
    private List<String> materialtypes = new ArrayList<>();
    private String extent = "";
    private String publisher = "";
    private List<String> catalogcodes = new ArrayList<>();
    private String title = "";
    private List<String> targetgroup = new ArrayList<>();
    private String metakompassubject = "";
    private String error = "";

    public String getFaust() {
        return faust;
    }

    public String getCreator() {
        return creator;
    }

    public List<String> getDk5() {
        return dk5;
    }

    public List<String> getIsbn() {
        return isbn;
    }

    public List<String> getMaterialtypes() {
        return materialtypes;
    }

    public String getExtent() {
        return extent;
    }

    public String getPublisher() {
        return publisher;
    }

    public List<String> getCatalogcodes() {
        return catalogcodes;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getTargetgroup() {
        return targetgroup;
    }

    public String getMetakompassubject() {
        return metakompassubject;
    }

    public String getError() {
        return error;
    }

    public boolean isOk() {
        return error.isEmpty();
    }

    public void setFaust(String faust) {
        this.faust = faust;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setDk5(List<String> dk5) {
        this.dk5 = dk5;
    }

    public void setIsbn(List<String> isbn) {
        this.isbn = isbn;
    }

    public void setMaterialtypes(List<String> materialtypes) {
        this.materialtypes = materialtypes;
    }

    public void setExtent(String extent) {
        this.extent = extent;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setCatalogcodes(List<String> catalogcodes) {
        this.catalogcodes = catalogcodes;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTargetgroup(List<String> targetgroup) {
        this.targetgroup = targetgroup;
    }

    public void setMetakompassubject(String metakompassubject) {
        this.metakompassubject = metakompassubject;
    }

    public void setError(String error) {
        this.error = error;
    }

    public BibliographicInformation withFaust(String faust) {
        this.faust = faust;
        return this;
    }

    public BibliographicInformation withCreator(String creator) {
        this.creator = creator;
        return this;
    }

    public BibliographicInformation withDk5(List<String> dk5) {
        this.dk5 = dk5;
        return this;
    }

    public BibliographicInformation withIsbn(List<String> isbn) {
        this.isbn = isbn;
        return this;
    }

    public BibliographicInformation withMaterialtypes(List<String> materialtypes) {
        this.materialtypes = materialtypes;
        return this;
    }

    public BibliographicInformation withExtent(String extent) {
        this.extent = extent;
        return this;
    }

    public BibliographicInformation withPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public BibliographicInformation withCatalogcodes(List<String> catalogcodes) {
        this.catalogcodes = catalogcodes;
        return this;
    }

    public BibliographicInformation withTitle(String title) {
        this.title = title;
        return this;
    }

    public BibliographicInformation withTargetgroup(List<String> targetgroup) {
        this.targetgroup = targetgroup;
        return this;
    }

    public BibliographicInformation withMetakompassubject(String metakompassubject) {
        this.metakompassubject = metakompassubject;
        return this;
    }

    public BibliographicInformation withError(String error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return "BibliographicInformation{" +
                "faust='" + faust + '\'' +
                ", creator='" + creator + '\'' +
                ", dk5='" + dk5 + '\'' +
                ", isbn='" + isbn + '\'' +
                ", materialtypes=" + materialtypes +
                ", extent='" + extent + '\'' +
                ", publisher='" + publisher + '\'' +
                ", catalogcodes=" + catalogcodes +
                ", title='" + title + '\'' +
                ", targetgroup='" + targetgroup + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
