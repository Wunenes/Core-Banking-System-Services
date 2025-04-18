package com.accountMicroservice.exceptions;

import lombok.Getter;

/**
 * Exception thrown when a requested account cannot be found in the system.
 * This is a checked exception as account lookup failures should be explicitly handled.
 */
@Getter
public class AccountNotFoundException extends Exception {
    /**
     * -- GETTER --
     *
     */
    private final String accountIdentifier;
    /**
     * -- GETTER --
     *
     */
    private final String searchCriteria;

    /**
     * Constructs a new exception with detail message and account identifier.
     * @param message the detail message
     * @param accountIdentifier the identifier used to search for the account (account number)
     */
    public AccountNotFoundException(String message, String accountIdentifier) {
        super(message);
        this.accountIdentifier = accountIdentifier;
        this.searchCriteria = null;
    }

    /**
     * Constructs a new exception with detail message, account identifier and search criteria.
     * @param message the detail message
     * @param accountIdentifier the identifier used to search for the user
     * @param searchCriteria the type of search performed (account number)
     */
    public AccountNotFoundException(String message, String accountIdentifier, String searchCriteria) {
        super(message);
        this.accountIdentifier = accountIdentifier;
        this.searchCriteria = searchCriteria;
    }

    /**
     * Constructs a standard error message including the search details
     * @return formatted error message
     */
    @Override
    public String getMessage() {
        if (searchCriteria != null) {
            return super.getMessage() +
                    " [Search Criteria: " + searchCriteria +
                    ", Identifier: " + accountIdentifier + "]";
        }
        return super.getMessage() + " [Identifier: " + accountIdentifier + "]";
    }
}
