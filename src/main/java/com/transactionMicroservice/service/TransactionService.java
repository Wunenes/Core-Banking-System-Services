package com.transactionMicroservice.service;

import com.transactionMicroservice.dto.TransactionRequestDTO;
import com.transactionMicroservice.dto.TransactionResponseDTO;
import com.transactionMicroservice.exceptions.IneligibleAccountException;
import com.transactionMicroservice.exceptions.InsufficientFundsException;


public interface TransactionService {
    TransactionResponseDTO internalTransfer(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException, InsufficientFundsException;
    TransactionResponseDTO processDeposit(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException;
}
