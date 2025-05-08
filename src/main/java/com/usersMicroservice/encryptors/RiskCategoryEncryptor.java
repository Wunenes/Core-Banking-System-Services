package com.usersMicroservice.encryptors;

import com.usersMicroservice.model.userDescription.RiskCategory;
import com.usersMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class RiskCategoryEncryptor implements AttributeConverter<RiskCategory, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(RiskCategory attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute.name());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public RiskCategory convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : RiskCategory.valueOf(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
