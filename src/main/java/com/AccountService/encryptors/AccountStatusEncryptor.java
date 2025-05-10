package com.AccountService.encryptors;

import com.AccountService.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.AccountService.model.AccountDescription.AccountStatus;

@Converter
@Component
public class AccountStatusEncryptor implements AttributeConverter<AccountStatus, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(AccountStatus attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute.name());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public AccountStatus convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : AccountStatus.valueOf(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
