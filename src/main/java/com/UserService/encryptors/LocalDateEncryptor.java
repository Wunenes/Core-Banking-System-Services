package com.UserService.encryptors;

import com.UserService.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Converter
@Component
public class LocalDateEncryptor implements AttributeConverter<LocalDate, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(String.valueOf(attribute));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : LocalDate.parse(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}