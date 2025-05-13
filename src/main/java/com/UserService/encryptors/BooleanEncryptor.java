package com.UserService.encryptors;

import com.UserService.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class BooleanEncryptor implements AttributeConverter<Boolean, String> {

    @Autowired
    private EncryptionService encryptionService;
    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(String.valueOf(attribute));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : Boolean.valueOf(encryptionService.encrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
