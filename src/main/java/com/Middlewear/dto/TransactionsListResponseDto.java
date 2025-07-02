package com.Middlewear.dto;

import com.TransactionService.grpc.TransactionsListResponse;

import java.util.List;
import java.util.stream.Collectors;

public class TransactionsListResponseDto {
    public List<TransactionResponseDto> transactions;

    public static TransactionsListResponseDto fromProto(TransactionsListResponse response) {
        TransactionsListResponseDto dto = new TransactionsListResponseDto();
        dto.transactions = response.getTransactionsList().stream()
            .map(TransactionResponseDto::fromProto)
            .collect(Collectors.toList());
        return dto;
    }
}
