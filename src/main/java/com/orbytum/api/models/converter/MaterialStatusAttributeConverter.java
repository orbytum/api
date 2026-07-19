package com.orbytum.api.models.converter;

import com.orbytum.api.models.enums.MaterialStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MaterialStatusAttributeConverter implements AttributeConverter<MaterialStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MaterialStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getId();
    }

    @Override
    public MaterialStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return MaterialStatus.fromId(dbData);
    }

}