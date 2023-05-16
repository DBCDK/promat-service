/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.promat.service.dto;

import dk.dbc.promat.service.persistence.MaterialType;

public class RecordMaterialTypeDto {

    private MaterialType materialType;
    private String specificType;

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public String getSpecificType() {
        return specificType;
    }

    public void setSpecificType(String specificType) {
        this.specificType = specificType;
    }

    public RecordMaterialTypeDto withMaterialType(MaterialType materialType) {
        this.materialType = materialType;
        return this;
    }

    public RecordMaterialTypeDto withSpecificType(String specificType) {
        this.specificType = specificType;
        return this;
    }

    @Override
    public String toString() {
        return "RecordMaterialTypeDto{" +
                "materialType=" + materialType +
                ", specificType='" + specificType + '\'' +
                '}';
    }
}
