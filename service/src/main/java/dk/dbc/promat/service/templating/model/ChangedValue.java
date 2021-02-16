package dk.dbc.promat.service.templating.model;

public class ChangedValue {
    private String fromValue;
    private String toValue;

    public String getFromValue() {
        return fromValue;
    }

    public String getToValue() {
        return toValue;
    }

    public ChangedValue withToValue(String toValue) {
        this.toValue = toValue;
        return this;
    }

    public ChangedValue withFromValue(String fromValue) {
        this.fromValue = fromValue;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChangedValue that = (ChangedValue) o;

        if (fromValue != null ? !fromValue.equals(that.fromValue) : that.fromValue != null) {
            return false;
        }
        return toValue != null ? toValue.equals(that.toValue) : that.toValue == null;
    }

    @Override
    public int hashCode() {
        int result = fromValue != null ? fromValue.hashCode() : 0;
        result = 31 * result + (toValue != null ? toValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChangedValue{" +
                "fromValue='" + fromValue + '\'' +
                ", toValue='" + toValue + '\'' +
                '}';
    }
}
