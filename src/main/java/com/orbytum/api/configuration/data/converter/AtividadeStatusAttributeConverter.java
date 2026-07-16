package com.orbytum.api.configuration.data.converter;

import com.orbytum.api.models.enums.AtividadeStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AtividadeStatusAttributeConverter implements AttributeConverter<AtividadeStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AtividadeStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getId();
    }

    @Override
    public AtividadeStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return AtividadeStatus.fromId(dbData);
    }

}