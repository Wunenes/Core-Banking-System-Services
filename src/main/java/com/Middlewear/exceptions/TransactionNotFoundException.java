package com.Middlewear.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested transaction cannot be found in the system.
 * This is a runtime exception as transaction lookup failures are typically
 * unrecoverable and indicate a client error.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
@Getter
public class TransactionNotFoundException extends RuntimeException {
    
    private final String transactionIdentifier;
    private final String searchCriteria;
    
    /**
     * Constructs a new exception with just a detail message.
     *
     * @param message the detail message
     */
    public TransactionNotFoundException(String message) {
        super(message);
        this.transactionIdentifier = null;
        this.searchCriteria = null;
    }
    
    /**
     * Constructs a new exception with a detail message and a cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.transactionIdentifier = null;
        this.searchCriteria = null;
    }
    
    /**
     * Constructs a new exception with transaction identifier and search criteria.
     *
     * @param transactionIdentifier the identifier used to search for the transaction
     * @param searchCriteria the type of search performed (e.g., "reference", "from_account")
     */
    public TransactionNotFoundException(String transactionIdentifier, String searchCriteria) {
        super(String.format("Transaction with %s '%s' not found", searchCriteria, transactionIdentifier));
        this.transactionIdentifier = transactionIdentifier;
        this.searchCriteria = searchCriteria;
    }
    
    /**
     * Constructs a detailed error message including transaction search details.
     *
     * @return formatted error message
     */
    @Override
    public String getMessage() {
        if (transactionIdentifier != null && searchCriteria != null) {
            return super.getMessage() +
                    " [Search Criteria: " + searchCriteria +
                    ", Identifier: " + transactionIdentifier + "]";
        }
        return super.getMessage();
    }
}