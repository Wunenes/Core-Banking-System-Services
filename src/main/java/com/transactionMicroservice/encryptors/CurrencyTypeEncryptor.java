package com.transactionMicroservice.encryptors;

import com.accountMicroservice.grpc.CurrencyType;
import com.transactionMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class CurrencyTypeEncryptor implements AttributeConverter<CurrencyType, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(CurrencyType attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute.name());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public CurrencyType convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : CurrencyType.valueOf(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
