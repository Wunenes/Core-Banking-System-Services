package com.TransactionService.service.Impl;

import com.AccountService.grpc.CreditResponse;
import com.AccountService.grpc.CurrencyType;
import com.AccountService.grpc.DebitResponse;
import org.springframework.stereotype.Service;
import com.TransactionService.client.AccountServiceClient;
import com.TransactionService.dto.TransactionRequestDTO;
import com.TransactionService.dto.TransactionResponseDTO;
import com.TransactionService.exceptions.*;
import com.TransactionService.model.Transaction;
import com.TransactionService.model.TransactionDescription;
import com.TransactionService.repository.TransactionRepository;
import com.TransactionService.service.TransactionMapper;
import com.TransactionService.service.TransactionService;
import com.TransactionService.util.TransactionNumberGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public TransactionResponseDTO getTransactionByReference(String transactionReference) {
        return transactionRepository.findByTransactionReference(transactionReference)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction with reference " +
                        transactionReference + " not found"));
    }

    @Override
    public List<TransactionResponseDTO> getTransactionsByFromAccount(String fromAccount) {
        List<Transaction> transactions = transactionRepository.findByFromAccount(fromAccount);

        return transactions.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<TransactionResponseDTO> getTransactionsByToAccount(String toAccount) {
        List<Transaction> transactions = transactionRepository.findByToAccount(toAccount);

        return transactions.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<TransactionResponseDTO> getTransactionByTransactionTime(LocalDateTime transactionTime) {
        List<Transaction> transactions = transactionRepository.findByTransactionTime(transactionTime);

        return transactions.stream()
                .map(this::toResponseDTO)
                .toList();
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
