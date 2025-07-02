package com.Middlewear.exceptions;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exception thrown when an account lacks sufficient funds for a requested transaction.
 * This is a checked exception as insufficient funds is a recoverable condition
 * that callers should explicitly handle (e.g., by notifying the user).
 */

@Getter
public class InsufficientFundsException extends Exception {
    private final String accountNumber;
    private final BigDecimal currentBalance;
    private final BigDecimal attemptedAmount;
    private final String currency;

    /**
     * Constructs a new exception with detailed financial information.
     *
     * @param message          Human-readable error message
     * @param accountNumber    The account that lacked funds
     * @param currentBalance   Current balance in the account
     * @param attemptedAmount  Amount that was attempted to be withdrawn/transferred
     * @param currency         Currency of the transaction
     */
    public InsufficientFundsException(String message,
                                      String accountNumber,
                                      BigDecimal currentBalance,
                                      BigDecimal attemptedAmount,
                                      String currency) {
        super(message);
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.attemptedAmount = attemptedAmount;
        this.currency = currency;
    }

    /**
     * Simplified constructor for cases where full details aren't available.
     */
    public InsufficientFundsException(String message) {
        this(message, null, null, null, null);
    }

    @Override
    public String getMessage() {
        if (accountNumber != null && currentBalance != null && attemptedAmount != null) {
            return String.format("%s [Account: %s, Current Balance: %.2f %s, Attempted: %.2f %s]",
                    super.getMessage(),
                    accountNumber,
                    currentBalance.doubleValue(),
                    currency,
                    attemptedAmount.doubleValue(),
                    currency);
        }
        return super.getMessage();
    }
}