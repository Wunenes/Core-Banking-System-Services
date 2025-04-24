package org.transactionMicroservice.service;

import org.transactionMicroservice.dto.TransactionRequestDTO;
import org.transactionMicroservice.dto.TransactionResponseDTO;
import org.transactionMicroservice.exceptions.IneligibleAccountException;
import org.transactionMicroservice.exceptions.InsufficientFundsException;


public interface TransactionService {
    TransactionResponseDTO internalTransfer(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException, InsufficientFundsException;
    TransactionResponseDTO processDeposit(TransactionRequestDTO transactionRequestDTO) throws IneligibleAccountException;
}
