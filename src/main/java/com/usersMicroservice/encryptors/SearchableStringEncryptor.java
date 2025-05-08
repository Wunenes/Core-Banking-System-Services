package com.usersMicroservice.encryptors;

import com.usersMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Converter
public class SearchableStringEncryptor implements AttributeConverter<String, String> {

    @Autowired
    private EncryptionService encryptionService;
    
    private static final Logger logger = LoggerFactory.getLogger(SearchableStringEncryptor.class);
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            if (attribute == null) return null;
            // Use deterministic encryption for searchable fields
            return encryptionService.encryptDeterministic(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Searchable field encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null) return null;
            return encryptionService.decrypt(dbData);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}