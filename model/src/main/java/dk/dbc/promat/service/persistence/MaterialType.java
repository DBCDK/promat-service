/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

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