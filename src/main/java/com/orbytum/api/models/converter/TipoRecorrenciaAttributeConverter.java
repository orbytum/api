package com.orbytum.api.models.converter;

import com.orbytum.api.models.enums.TipoRecorrencia;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoRecorrenciaAttributeConverter implements AttributeConverter<TipoRecorrencia, String> {

    @Override
    public String convertToDatabaseColumn(TipoRecorrencia attribute) {
        if (attribute == null) {
            return TipoRecorrencia.NENHUMA.name();
        }
        return attribute.name();
    }

    @Override
    public TipoRecorrencia convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return TipoRecorrencia.NENHUMA;
        }
        return TipoRecorrencia.fromString(dbData);
    }
}
