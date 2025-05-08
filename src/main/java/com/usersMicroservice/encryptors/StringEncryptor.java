package com.usersMicroservice.encryptors;

import com.usersMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
@Converter
public class StringEncryptor implements AttributeConverter<String, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : encryptionService.decrypt(dbData);
        } catch (Exception e) {
            log.info("Decryption failed");
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
