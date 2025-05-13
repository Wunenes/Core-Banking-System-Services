package com.UserService.encryptors;

import com.UserService.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Converter
@Component
public class LocalDateTimeEncryptor implements AttributeConverter<LocalDateTime, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(String.valueOf(attribute));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : LocalDateTime.parse(encryptionService.encrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
