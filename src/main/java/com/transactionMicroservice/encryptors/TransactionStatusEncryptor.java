package com.transactionMicroservice.encryptors;

import com.transactionMicroservice.model.TransactionDescription.TransactionStatus;
import com.transactionMicroservice.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class TransactionStatusEncryptor implements AttributeConverter<TransactionStatus, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(TransactionStatus attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute.name());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public TransactionStatus convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : TransactionStatus.valueOf(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
