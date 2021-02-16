package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Reviewer;
import dk.dbc.promat.service.templating.model.ChangedValue;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReviewerDiffer {
    private final Set<String> changeableFields = Set.of(
            "active", "firstName", "lastName", "email", "phone", "institution", "paycode",
            "hiatus_begin", "hiatus_end", "capacity");

    private final Set<String> changeableAddressFields = Set.of(
            "address1", "address2", "zip", "city");

    private <T, T1> Map<String, ChangedValue> getChangedValueMap(T1 fromObject, T toObject, Set<String> fieldsTocheck )
            throws IllegalAccessException {

        Map<String, Field> reviewerRequestFields = getFields(toObject, fieldsTocheck);
        Map<String, Field> reviewerFields = getFields(fromObject, fieldsTocheck);

        Map<String, ChangedValue> changedValueMap = new HashMap<>();
        for (String fieldName : fieldsTocheck) {
            Field existing = reviewerFields.get(fieldName);
            Field newOne = reviewerRequestFields.get(fieldName);
            newOne.setAccessible(true);
            if (newOne.get(toObject) != null) {
                existing.setAccessible(true);
                if (!newOne.get(toObject).equals(existing.get(fromObject))) {
                    changedValueMap.put(fieldName,
                            new ChangedValue()
                                    .withFromValue(getValueAsString(existing, fromObject))
                                    .withToValue(getValueAsString(newOne, toObject))
                    );
                }
            }
            newOne.setAccessible(false);
        }
        return changedValueMap;
    }

    public Map<String, ChangedValue> getChangedValueMap(Reviewer reviewer, ReviewerRequest reviewerRequest) throws IllegalAccessException {
        Map<String, ChangedValue> valueMap = getChangedValueMap(reviewer, reviewerRequest, changeableFields);
        if (reviewerRequest.getAddress() != null) {
            valueMap.putAll(getChangedValueMap(reviewer.getAddress(), reviewerRequest.getAddress(), changeableAddressFields));
        }
        return valueMap;
    }

    private String getValueAsString(Field f, Object o ) throws IllegalAccessException {
        if (o == null || f == null || f.get(o) == null) {  return null; }
        switch (f.getType().getName()) {
            case "java.lang.String":
                return (String) f.get(o);
            case "java.lang.Integer":
                return String.valueOf(f.get(o));
            case "java.time.LocalDate":
                return Formatting.format((LocalDate) f.get(o));
            case "java.lang.Boolean":
            case "boolean":
                return f.get(o)+"";

            default:
                throw new IllegalAccessException(String.format("Type <%s>", f.getType().getName()));
        }
    }

    private <T> Map<String, Field> getFields(T t, Set<String> fieldNames) {
        Set<Field> fields = new HashSet<>();
        if (t == null) {
            return new HashMap<>() {
            };
        }
        Class clazz = t.getClass();
        while (clazz != Object.class) {
            fields.addAll(Set.of(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.stream().filter(field -> fieldNames.contains(field.getName())).collect(Collectors.toMap(Field::getName, field -> field));
    }
}
