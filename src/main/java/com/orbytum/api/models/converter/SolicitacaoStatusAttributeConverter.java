package com.orbytum.api.models.converter;

import com.orbytum.api.models.enums.SolicitacaoStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SolicitacaoStatusAttributeConverter implements AttributeConverter<SolicitacaoStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SolicitacaoStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getId();
    }

    @Override
    public SolicitacaoStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return SolicitacaoStatus.fromId(dbData);
    }

}
