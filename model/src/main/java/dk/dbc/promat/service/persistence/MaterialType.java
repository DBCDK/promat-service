package dk.dbc.promat.service.persistence;

import java.util.HashMap;
import java.util.Map;

public enum MaterialType {
    UNKNOWN("UNKNOWN"),
    BOOK("BOOK"),
    MOVIE("MOVIE"),
    MULTIMEDIA("MULTIMEDIA");

    private String materialName;
    MaterialType(String materialName) {
        this.materialName = materialName;
    }

    private static final Map<String, MaterialType> lookup = new HashMap();

    static {
        for (MaterialType materialType : MaterialType.values()) {
            lookup.put(materialType.materialName, materialType);
        }
    }

    public static MaterialType of(String materialName) {
        return lookup.get(materialName);
    }
}
