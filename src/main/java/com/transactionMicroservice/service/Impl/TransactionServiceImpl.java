package com.transactionMicroservice.service.Impl;

import com.accountMicroservice.grpc.CreditResponse;
import com.accountMicroservice.grpc.CurrencyType;
import com.accountMicroservice.grpc.DebitResponse;
import org.springframework.stereotype.Service;
import com.transactionMicroservice.client.AccountServiceClient;
import com.transactionMicroservice.dto.TransactionRequestDTO;
import com.transactionMicroservice.dto.TransactionResponseDTO;
import com.transactionMicroservice.exceptions.IneligibleAccountException;
import com.transactionMicroservice.exceptions.InsufficientFundsException;
import com.transactionMicroservice.model.Transaction;
import com.transactionMicroservice.model.TransactionDescription;
import com.transactionMicroservice.repository.TransactionRepository;
import com.transactionMicroservice.service.TransactionMapper;
import com.transactionMicroservice.service.TransactionService;
import com.transactionMicroservice.util.TransactionNumberGenerator;

import java.math.BigDecimal;

@Service
public class TransactionServiceImpl implements TransactionService, TransactionMapper {
    AccountServiceClient accountServiceClient;
    TransactionRepository transactionRepository;

    public TransactionResponseDTO internalTransfer(TransactionRequestDTO request) throws IneligibleAccountException, InsufficientFundsException {
        try {
            String fromAccountNumber = request.getFromAccount();
            String toAccountNumber = request.getToAccount();
            BigDecimal amount = request.getAmount();
            CurrencyType currencyType = request.getCurrencyType();

            DebitResponse debitResponse = accountServiceClient.debitAccount(fromAccountNumber, amount, currencyType);
            CreditResponse creditResponse = accountServiceClient.creditAccount(toAccountNumber, amount, currencyType);

            Transaction transaction = toModel(request);

            transaction.setStatus(TransactionDescription.TransactionStatus.COMPLETED);
            transaction.setDebitBalanceAfterTransaction(new BigDecimal(debitResponse.getNewBalance()));
            transaction.setCreditBalanceAfterTransaction(new BigDecimal(creditResponse.getNewBalance()));
            transaction.setTransactionReference(generateTransactionReference(transaction.getTransactionType(), transaction.getStatus()));

            transactionRepository.save(transaction);

            return toResponseDTO(transaction);

        } catch(IneligibleAccountException e) {
            Transaction transaction = toModel(request);
            transaction.setStatus(TransactionDescription.TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw e;
        } catch(InsufficientFundsException e) {
            Transaction transaction = toModel(request);
            transaction.setStatus(TransactionDescription.TransactionStatus.FAILED);
            transaction.setRejectionReason(e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException("Deposit failed: " + e.getMessage(), e);
        }
    }

    public TransactionResponseDTO processDeposit(TransactionRequestDTO request) throws IneligibleAccountException {
        try {
            String toAccountNumber = request.getToAccount();
            BigDecimal amount = request.getAmount();
            CurrencyType currencyType = request.getCurrencyType();

            // Validate amount is positive
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Deposit amount must be positive");
            }

            // Credit the customer's account
            CreditResponse creditResponse = accountServiceClient.creditAccount(
                    toAccountNumber,
                    amount,
                    currencyType
            );

            Transaction transaction = toModel(request);
            transaction.setStatus(TransactionDescription.TransactionStatus.COMPLETED);
            transaction.setCreditBalanceAfterTransaction(new BigDecimal(creditResponse.getNewBalance()));
            transaction.setTransactionReference(generateTransactionReference(transaction.getTransactionType(), transaction.getStatus()));
            transactionRepository.save(transaction);

            return toResponseDTO(transaction);

        } catch(IneligibleAccountException e) {
            Transaction transaction = toModel(request);
            transaction.setStatus(TransactionDescription.TransactionStatus.FAILED);
            transaction.setRejectionReason(e.getMessage());
            transactionRepository.save(transaction);
            throw e;

        } catch (Exception e) {
            throw new RuntimeException("Deposit failed: " + e.getMessage(), e);
        }
    }

    private String generateTransactionReference(TransactionDescription.TransactionType transactionType, TransactionDescription.TransactionStatus status) {
        return TransactionNumberGenerator.generate(transactionType, status);
    }

    @Override
    public TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .amount(transaction.getAmount())
                .currencyType(transaction.getCurrencyType())
                .transactionTime(transaction.getTransactionTime())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .transactionType(transaction.getTransactionType())
                .feeAmount(transaction.getFeeAmount())
                .feeCurrencyType(transaction.getFeeCurrencyType())
                .initiatedBy(transaction.getInitiatedBy())
                .processingDate(transaction.getProcessingDate())
                .valueDate(transaction.getValueDate())
                .debitBalanceAfterTransaction(transaction.getDebitBalanceAfterTransaction())
                .creditBalanceAfterTransaction(transaction.getCreditBalanceAfterTransaction())
                .rejectionReason(transaction.getRejectionReason())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    @Override
    public Transaction toModel(TransactionRequestDTO request) {
        Transaction transaction = new Transaction();
        transaction.setFromAccount(request.getFromAccount());
        transaction.setToAccount(request.getToAccount());
        transaction.setAmount(request.getAmount());
        transaction.setCurrencyType(request.getCurrencyType());
        transaction.setDescription(request.getDescription());
        transaction.setInitiatedBy(request.getInitiatedBy());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setValueDate(request.getValueDate());

        return transaction;
    }
}
