package com.TransactionService.service;

import com.TransactionService.dto.TransactionRequestDTO;
import com.TransactionService.dto.TransactionResponseDTO;
import com.TransactionService.exceptions.IneligibleAccountException;
import com.TransactionService.exceptions.InsufficientFundsException;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    TransactionResponseDTO internalTransfer(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException, InsufficientFundsException;
    TransactionResponseDTO processDeposit(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException;
    TransactionResponseDTO getTransactionByReference(String transactionReference);
    List<TransactionResponseDTO> getTransactionsByFromAccount(String fromAccount);
    List<TransactionResponseDTO> getTransactionsByToAccount(String toAccount);
    List<TransactionResponseDTO> getTransactionByTransactionTime(LocalDateTime transactionTime);
}
