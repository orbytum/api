package com.orbytum.api.models.converter;

import com.orbytum.api.models.enums.TipoLembrete;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoLembreteAttributeConverter implements AttributeConverter<TipoLembrete, String> {

    @Override
    public String convertToDatabaseColumn(TipoLembrete attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public TipoLembrete convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return TipoLembrete.fromString(dbData);
    }
}
