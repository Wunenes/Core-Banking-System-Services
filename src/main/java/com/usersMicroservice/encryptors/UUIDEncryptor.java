package com.usersMicroservice.encryptors;

import com.usersMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Converter
@Component
public class UUIDEncryptor implements AttributeConverter<UUID, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(UUID attribute) {
        try {
            return attribute == null ? null : encryptionService.encryptDeterministic(attribute.toString());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public UUID convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : UUID.fromString(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
