package com.TransactionService.service;

import com.TransactionService.dto.TransactionRequestDTO;
import com.TransactionService.dto.TransactionResponseDTO;
import com.TransactionService.exceptions.IneligibleAccountException;
import com.TransactionService.exceptions.InsufficientFundsException;


public interface TransactionService {
    TransactionResponseDTO internalTransfer(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException, InsufficientFundsException;
    TransactionResponseDTO processDeposit(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException;
}
