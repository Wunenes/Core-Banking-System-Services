package com.TransactionService.util;

import com.TransactionService.model.TransactionDescription;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionNumberGenerator {
    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 1000);

    public static String generate(TransactionDescription.TransactionType type) {
        String prefix = String.valueOf(getChar(type));

        // 2. Random Alphanumeric (8 chars)
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            randomPart.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }

        // 3. Sequence Number (Anti-collision)
        String sequence = Long.toString(counter.getAndIncrement(), 36).toUpperCase();

        // 4. Combine and add check digit
        String base = prefix + randomPart + "-" + sequence;
        return base + calculateLuhnCheckDigit(base);
    }

    private static char getChar(TransactionDescription.TransactionType type) {
        char TRANSACTIONTYPE = 0;
        switch (type){
            case DEPOSIT -> TRANSACTIONTYPE = 'D';
            case WITHDRAWAL -> TRANSACTIONTYPE = 'W';
            case INTERNAL -> TRANSACTIONTYPE = 'I';
            case EXTERNAL -> TRANSACTIONTYPE = 'E';
        }

        return TRANSACTIONTYPE;
    }

    private static int calculateLuhnCheckDigit(String input) {
        String clean = input.replaceAll("[^A-Z0-9]", "");
        int sum = 0;
        boolean alternate = false;
        for (int i = clean.length() - 1; i >= 0; i--) {
            char c = clean.charAt(i);
            int n = Character.isDigit(c) ? Character.getNumericValue(c) : (c - 'A' + 10);
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n / 10) + (n % 10);
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

}