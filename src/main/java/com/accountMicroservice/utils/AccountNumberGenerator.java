package com.accountMicroservice.utils;

import com.accountMicroservice.model.AccountDescription;

import java.security.SecureRandom;

public class AccountNumberGenerator {
    // Bank configuration
    private static String ACCOUNT_CODE = null;
    private static final SecureRandom random = new SecureRandom();

    public static String generate(AccountDescription.AccountType type) {
        switch (type) {
            case LOAN -> ACCOUNT_CODE = "01";
            case SAVINGS -> ACCOUNT_CODE = "02";
            case FOREIGN -> ACCOUNT_CODE = "03";
            case CHECKING -> ACCOUNT_CODE = "04";
        }

        // 1. Build base number (without checksum)
        String baseNumber = "00" // Branch code (simplified)
                + ACCOUNT_CODE
                + String.format("%04d", random.nextInt(100000));

        // 2. Add Luhn checksum
        return baseNumber + calculateLuhnChecksum(baseNumber);
    }

    // Luhn algorithm for checksum
    private static int calculateLuhnChecksum(String base) {
        int sum = 0;
        boolean alternate = false;
        for (int i = base.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(base.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) digit = (digit % 10) + 1;
            }
            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }
}