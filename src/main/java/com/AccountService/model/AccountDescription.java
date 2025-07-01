package com.AccountService.model;

public class AccountDescription {
    public enum AccountStatus{
        ACTIVE,
        INACTIVE,
        FROZEN,
        DORMANT,
        CLOSED,
    }

    public enum AccountType{
        CHECKING,
        SAVINGS,
        LOAN,
        FOREIGN,
        INTERNAL
    }

    public enum CurrencyType{
        KES,
        USD,
        EUR,
        GBP
    }
}
