package com.AccountService.encryptors;

import com.AccountService.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.AccountService.model.AccountDescription.AccountType;

@Converter
@Component
public class AccountTypeEncryptor implements AttributeConverter<AccountType, String> {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public String convertToDatabaseColumn(AccountType attribute) {
        try {
            return attribute == null ? null : encryptionService.encrypt(attribute.name());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public AccountType convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : AccountType.valueOf(encryptionService.decrypt(dbData));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
