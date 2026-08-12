package com.orbytum.api.models.converter;

import com.orbytum.api.models.enums.Permissao;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PermissaoAttributeConverter implements AttributeConverter<Permissao, String> {

    @Override
    public String convertToDatabaseColumn(Permissao attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getKey();
    }

    @Override
    public Permissao convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Permissao.fromKey(dbData);
    }
}