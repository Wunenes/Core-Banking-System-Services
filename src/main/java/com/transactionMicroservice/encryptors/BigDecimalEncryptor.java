package com.transactionMicroservice.encryptors;

import com.transactionMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Converter
@Component
public class BigDecimalEncryptor implements AttributeConverter<BigDecimal, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute.toPlainString());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : new BigDecimal(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
