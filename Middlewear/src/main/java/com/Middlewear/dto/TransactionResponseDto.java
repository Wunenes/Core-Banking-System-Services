package com.Middlewear.dto;

import com.TransactionService.grpc.TransactionResponse;

public class TransactionResponseDto {
    public String status;
    public String transactionReference;
    public String timestamp;
    public String fromAccount;
    public String toAccount;
    public String amount;
    public String currencyType;
    public String feeAmount;
    public String feeCurrency;
    public String transactionType;
    public String description;

    public static TransactionResponseDto fromProto(TransactionResponse response) {
        TransactionResponseDto dto = new TransactionResponseDto();
        dto.status = response.getTransactionStatus();
        dto.transactionReference = response.getTransactionReference();
        dto.timestamp = response.getTimestamp();
        dto.fromAccount = response.getFromAccount();
        dto.toAccount = response.getToAccount();
        dto.amount = response.getAmount();
        dto.feeAmount = response.getFeeAmount();
        dto.feeCurrency = response.getFeeCurrency();
        dto.currencyType = response.getCurrencyType();
        dto.transactionType = response.getTransactionType();
        dto.description = response.getDescription();
        return dto;
    }
}
