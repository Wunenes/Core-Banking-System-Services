package com.AccountService.exceptions;

import lombok.Getter;

/**
 * Exception thrown when an operation is attempted on a frozen account.
 * This is a checked exception because it represents a business rule violation that should be explicitly handled.
 */
@Getter
public class IneligibleAccountException extends Exception {
    private final String accountId;
    private final String attemptedOperation;

    /**
     * Constructs a new exception with a detail message and account ID.
     *
     * @param message the detail message
     * @param accountId the ID of the frozen account
     */
    public IneligibleAccountException(String message, String accountId) {
        super(message);
        this.accountId = accountId;
        this.attemptedOperation = null;
    }

    /**
     * Constructs a new exception with a detail message, account ID, and the operation that was attempted.
     *
     * @param message the detail message
     * @param accountNumber the ID of the frozen account
     * @param attemptedOperation the operation attempted on the frozen account
     */
    public IneligibleAccountException(String message, String accountNumber, String attemptedOperation) {
        super(message);
        this.accountId = accountNumber;
        this.attemptedOperation = attemptedOperation;
    }

    /**
     * Constructs a detailed error message including account and operation info.
     *
     * @return formatted error message
     */
    @Override
    public String getMessage() {
        if (attemptedOperation != null) {
            return super.getMessage() +
                    " [Account ID: " + accountId +
                    ", Attempted Operation: " + attemptedOperation + "]";
        }
        return super.getMessage() + " [Account ID: " + accountId + "]";
    }
}
