package com.orbytum.api.models.converter;

import com.orbytum.api.models.enums.NoticiaStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class NoticiaStatusAttributeConverter implements AttributeConverter<NoticiaStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(NoticiaStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getId();
    }

    @Override
    public NoticiaStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return NoticiaStatus.fromId(dbData);
    }
}
