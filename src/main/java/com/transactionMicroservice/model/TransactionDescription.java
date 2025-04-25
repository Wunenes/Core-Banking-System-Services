package com.transactionMicroservice.model;

public class TransactionDescription {
    public enum TransactionType{
        INTERNAL, EXTERNAL, DEPOSIT, WITHDRAWAL
    }

    public enum TransactionStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REVERSED
    }
}
