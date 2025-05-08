package com.usersMicroservice.encryptors;

import com.usersMicroservice.model.userDescription.GovernmentIdType;
import com.usersMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class GovernmentIdTypeEncryptor implements AttributeConverter<GovernmentIdType, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(GovernmentIdType attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute.name());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public GovernmentIdType convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : GovernmentIdType.valueOf(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
