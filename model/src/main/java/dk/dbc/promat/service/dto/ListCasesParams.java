/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.CaseStatus;

import java.util.HashMap;

public class ListCasesParams extends HashMap<String, Object> {
    public enum Key {
        /**
         * ID of editor assigned to cases
         */
        EDITOR("editor"),
        /**
         * ID of creator of case
         */
        CREATOR("creator"),
        /**
         * Faust number that must be covered by the cases returned
         */
        FAUST("faust"),
        /**
         * View format for returned cases
         */
        FORMAT("format"),
        /**
         * Return cases with an id greater than this number
         */
        FROM("from"),
        /**
         * Return cases with an id less than this number
         */
        TO("to"),
        /**
         * Maximum number of cases returned
         */
        LIMIT("limit"),
        /**
         * ID of reviewer assigned to cases
         */
        REVIEWER("reviewer"),
        /**
         * One or more case statuses
         */
        STATUS("status"),
        /**
         * Title (or part of) for cases
         */
        TITLE("title"),
        /**
         * Trimmed weekcode for cases
         */
        TRIMMED_WEEKCODE("trimmedWeekcode"),
        /**
         * Trimmed weekcode comparison operator
         */
        TRIMMED_WEEKCODE_OPERATOR("trimmedWeekcodeOperator"),
        /**
         * Weekcode for cases
         */
        WEEKCODE("weekCode"),
        /**
         * Name of author
         */
        AUTHOR("author"),
        /**
         * Materials (BOOK, MOVIE, MULTIMEDIA)
         */
        MATERIALS("materials"),
        /**
         * How to order search results (ascending/descending)
         */
        ORDER("order");

        private final String keyName;

        Key(String keyName) {
            this.keyName = keyName;
        }

        public String getKeyName() {
            return keyName;
        }
    }

    public enum Format {
        EXPORT, SUMMARY;
    }

    public enum Order {
        ASCENDING, DESCENDING;
    }

    public ListCasesParams withEditor(Integer id) {
        return withInteger(Key.EDITOR, id);
    }

    public Integer getEditor() {
        return getInteger(Key.EDITOR);
    }

    public ListCasesParams withCreator(Integer id) {
        return withInteger(Key.CREATOR, id);
    }

    public Integer getCreator() { return getInteger(Key.CREATOR); }

    public ListCasesParams withFaust(String faust) {
        return withString(Key.FAUST, faust);
    }

    public String getFaust() {
        return getString(Key.FAUST);
    }

    public ListCasesParams withFormat(Format format) {
        putOrRemoveOnNull(Key.FORMAT, format);
        return this;
    }

    public Format getFormat() {
        final Object value = this.get(Key.FORMAT);
        if (value != null) {
            return (Format) value;
        }
        return null;
    }

    public ListCasesParams withFrom(Integer from) {
        return withInteger(Key.FROM, from);
    }

    public Integer getFrom() {
        return getInteger(Key.FROM);
    }

    public ListCasesParams withTo(Integer to) {
        return withInteger(Key.TO, to);
    }

    public Integer getTo() {
        return getInteger(Key.TO);
    }

    public ListCasesParams withLimit(Integer limit) {
        return withInteger(Key.LIMIT, limit);
    }

    public Integer getLimit() {
        return getInteger(Key.LIMIT);
    }

    public ListCasesParams withReviewer(Integer id) {
        return withInteger(Key.REVIEWER, id);
    }

    public Integer getReviewer() {
        return getInteger(Key.REVIEWER);
    }

    public ListCasesParams withStatus(CaseStatus status) {
        final String oldValue = getStatus();
        final String newValue;
        if (oldValue != null) {
            newValue = String.join(",", oldValue, status.name());
        } else {
            newValue = status.name();
        }
        putOrRemoveOnNull(Key.STATUS, newValue);
        return this;
    }

    public ListCasesParams withStatus(String status) {
        putOrRemoveOnNull(Key.STATUS, status);
        return this;
    }

    public String getStatus() {
        return getString(Key.STATUS);
    }

    public ListCasesParams withTitle(String title) {
        return withString(Key.TITLE, title);
    }

    public String getTitle() {
        return getString(Key.TITLE);
    }

    public ListCasesParams withAuthor(String author) {
        return withString(Key.AUTHOR, author);
    }

    public String getAuthor() {
        return getString(Key.AUTHOR);
    }

    public ListCasesParams withTrimmedWeekcode(String trimmedWeekcode) {
        return withString(Key.TRIMMED_WEEKCODE, trimmedWeekcode);
    }

    public String getTrimmedWeekcode() {
        return getString(Key.TRIMMED_WEEKCODE);
    }

    public ListCasesParams withTrimmedWeekcodeOperator(CriteriaOperator trimmedWeekcodeOperator) {
        putOrRemoveOnNull(Key.TRIMMED_WEEKCODE_OPERATOR, trimmedWeekcodeOperator);
        return this;
    }

    public CriteriaOperator getTrimmedWeekcodeOperator() {
        final Object value = this.get(Key.TRIMMED_WEEKCODE_OPERATOR);
        if (value != null) {
            return (CriteriaOperator) value;
        }
        return null;
    }

    public ListCasesParams withWeekCode(String weekcode) {
        return withString(Key.WEEKCODE, weekcode);
    }

    public String getWeekCode() {
        return getString(Key.WEEKCODE);
    }

    public ListCasesParams withMaterials(String materials) {
        return withString(Key.MATERIALS, materials);
    }

    public String getMaterials() {
        return getString(Key.MATERIALS);
    }

    public ListCasesParams withOrder(Order order) {
        putOrRemoveOnNull(Key.ORDER, order);
        return this;
    }

    public Order getOrder() {
        final Object value = this.get(Key.ORDER);
        if (value != null) {
            return (Order) value;
        }
        return null;
    }

    private void putOrRemoveOnNull(Key param, Object value) {
        if (value == null) {
            this.remove(param.keyName);
        } else {
            this.put(param.keyName, value);
        }
    }

    private Object get(Key param) {
        return get(param.keyName);
    }

    private ListCasesParams withInteger(Key key, Integer value) {
        putOrRemoveOnNull(key, value);
        return this;
    }

    public Integer getInteger(Key key) {
        final Object value = this.get(key);
        if (value != null) {
            return (Integer) value;
        }
        return null;
    }

    private ListCasesParams withString(Key key, String value) {
        putOrRemoveOnNull(key, value);
        return this;
    }

    public String getString(Key key) {
        final Object value = this.get(key);
        if (value != null) {
            return (String) value;
        }
        return null;
    }
}
