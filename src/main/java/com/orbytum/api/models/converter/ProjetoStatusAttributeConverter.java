package com.orbytum.api.models.converter;

import com.orbytum.api.models.enums.ProjetoStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProjetoStatusAttributeConverter implements AttributeConverter<ProjetoStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProjetoStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getId();
    }

    @Override
    public ProjetoStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ProjetoStatus.fromId(dbData);
    }

}