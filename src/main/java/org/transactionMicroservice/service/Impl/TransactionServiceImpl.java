package org.transactionMicroservice.service.Impl;

import com.accountMicroservice.grpc.CreditResponse;
import com.accountMicroservice.grpc.CurrencyType;
import com.accountMicroservice.grpc.DebitResponse;
import org.transactionMicroservice.client.AccountServiceClient;
import org.transactionMicroservice.dto.TransactionRequestDTO;
import org.transactionMicroservice.dto.TransactionResponseDTO;
import org.transactionMicroservice.exceptions.IneligibleAccountException;
import org.transactionMicroservice.exceptions.InsufficientFundsException;
import org.transactionMicroservice.model.Transaction;
import org.transactionMicroservice.model.TransactionDescription;
import org.transactionMicroservice.repository.TransactionRepository;
import org.transactionMicroservice.service.TransactionMapper;
import org.transactionMicroservice.service.TransactionService;

import java.math.BigDecimal;

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
