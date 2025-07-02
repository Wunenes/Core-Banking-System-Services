package com.Middlewear.controller;

import com.Middlewear.client.TransactionServiceClient;
import com.Middlewear.dto.TransactionRequestDto;
import com.Middlewear.dto.TransactionResponseDto;
import com.Middlewear.dto.TransactionsListResponseDto;
import com.TransactionService.grpc.TransactionResponse;
import com.TransactionService.grpc.TransactionsListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionServiceClient transactionServiceClient;

    public TransactionController(TransactionServiceClient transactionServiceClient) {
        this.transactionServiceClient = transactionServiceClient;
    }

    @PostMapping("/internal-transfer")
    public ResponseEntity<?> internalTransfer(@RequestBody TransactionRequestDto requestDto) {
        try {
            TransactionResponse response = transactionServiceClient.internalTransfer(
                requestDto.fromAccount,
                requestDto.toAccount,
                requestDto.amount,
                requestDto.currencyType,
                requestDto.transactionType,
                requestDto.description,
                requestDto.initiatedBy
            );
            return ResponseEntity.ok(TransactionResponseDto.fromProto(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> processDeposit(@RequestBody TransactionRequestDto requestDto) {
        try {
            TransactionResponse response = transactionServiceClient.processDeposit(
                requestDto.fromAccount,
                requestDto.toAccount,
                requestDto.amount,
                requestDto.currencyType,
                requestDto.description
            );
            return ResponseEntity.ok(TransactionResponseDto.fromProto(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/reference/{ref}")
    public ResponseEntity<?> getTransactionByReference(@PathVariable String ref) {
        try {
            TransactionResponse response = transactionServiceClient.getTransactionByReference(ref);
            return ResponseEntity.ok(TransactionResponseDto.fromProto(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getTransactionsByAccount(@PathVariable String accountId,
                                                      @RequestParam int page,
                                                      @RequestParam int size) {
        try {
            TransactionsListResponse response = transactionServiceClient.getTransactionsByAccountId(accountId, page, size);
            return ResponseEntity.ok(TransactionsListResponseDto.fromProto(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/from-account/{accountId}")
    public ResponseEntity<?> getTransactionsByFromAccount(@PathVariable String accountId) {
        try {
            TransactionsListResponse response = transactionServiceClient.getTransactionsByFromAccount(accountId);
            return ResponseEntity.ok(TransactionsListResponseDto.fromProto(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/to-account/{accountId}")
    public ResponseEntity<?> getTransactionsByToAccount(@PathVariable String accountId) {
        try {
            TransactionsListResponse response = transactionServiceClient.getTransactionsByToAccount(accountId);
            return ResponseEntity.ok(TransactionsListResponseDto.fromProto(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/time")
    public ResponseEntity<?> getTransactionsByTime(@RequestParam String transactionTime) {
        try {
            TransactionsListResponse response = transactionServiceClient.getTransactionByTransactionTime(transactionTime);
            return ResponseEntity.ok(TransactionsListResponseDto.fromProto(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
