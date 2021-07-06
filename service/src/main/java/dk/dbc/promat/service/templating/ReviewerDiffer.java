package dk.dbc.promat.service.templating;

import dk.dbc.promat.service.dto.ReviewerRequest;
import dk.dbc.promat.service.persistence.Address;
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
    private static class PrivateAddress {
        private String privateAddress1;
        private String privateAddress2;
        private String privateZip;
        private String privateCity;
        private Boolean privateSelected;

        public PrivateAddress(Address address) {
            if (address != null) {
                this.privateAddress1 = address.getAddress1();
                this.privateAddress2 = address.getAddress2();
                this.privateCity = address.getCity();
                this.privateZip = address.getZip();
                this.privateSelected = address.getSelected();
            }
        }

        public String getPrivateAddress1() {
            return privateAddress1;
        }

        public void setPrivateAddress1(String privateAddress1) {
            this.privateAddress1 = privateAddress1;
        }

        public String getPrivateAddress2() {
            return privateAddress2;
        }

        public void setPrivateAddress2(String privateAddress2) {
            this.privateAddress2 = privateAddress2;
        }

        public String getPrivateZip() {
            return privateZip;
        }

        public void setPrivateZip(String privateZip) {
            this.privateZip = privateZip;
        }

        public String getPrivateCity() {
            return privateCity;
        }

        public void setPrivateCity(String privateCity) {
            this.privateCity = privateCity;
        }

        public Boolean getPrivateSelected() {
            return privateSelected;
        }

        public void setPrivateSelected(Boolean privateSelected) {
            this.privateSelected = privateSelected;
        }
    }

    private final Set<String> changeableFields = Set.of(
            "active", "firstName", "lastName", "email", "phone", "institution", "paycode",
            "hiatusBegin", "hiatusEnd", "capacity", "privateEmail");

    private final Set<String> changeableAddressFields = Set.of(
            "address1", "address2", "zip", "city", "selected");

    private final Set<String> changeablePrivateAddressFields = Set.of(
            "privateAddress1", "privateAddress2", "privateCity", "privateZip", "privateSelected");

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
            valueMap.putAll(getChangedValueMap(
                    reviewer.getAddress() == null ? new Address() : reviewer.getAddress(),
                    reviewerRequest.getAddress(),
                    changeableAddressFields));
        }
        if (reviewerRequest.getPrivateAddress() != null) {
            PrivateAddress privateAddressRequest = new PrivateAddress(reviewerRequest.getPrivateAddress());
            PrivateAddress privateAddress = new PrivateAddress(reviewer.getPrivateAddress());
            valueMap.putAll(getChangedValueMap(
                    privateAddress,
                    privateAddressRequest,
                    changeablePrivateAddressFields
                    ));
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
