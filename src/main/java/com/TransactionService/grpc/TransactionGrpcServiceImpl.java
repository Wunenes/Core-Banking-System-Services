package com.TransactionService.grpc;

import com.AccountService.grpc.CurrencyType;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import com.TransactionService.dto.TransactionRequestDTO;
import com.TransactionService.dto.TransactionResponseDTO;
import com.TransactionService.exceptions.IneligibleAccountException;
import com.TransactionService.exceptions.InsufficientFundsException;
import com.TransactionService.exceptions.TransactionNotFoundException;
import com.TransactionService.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class TransactionGrpcServiceImpl extends TransactionServiceGrpc.TransactionServiceImplBase {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private TransactionService transactionService;

    @Override
    public void internalTransfer(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            // Convert gRPC request to DTO
            TransactionRequestDTO requestDTO = TransactionRequestDTO.builder()
                    .fromAccount(request.getFromAccount())
                    .toAccount(request.getToAccount())
                    .amount(new BigDecimal(request.getAmount()))
                    .currencyType(CurrencyType.valueOf(request.getCurrencyType()))
                    .description(request.getDescription())
                    .build();
            
            // Process the transaction
            TransactionResponseDTO responseDTO = transactionService.internalTransfer(requestDTO);
            
            // Convert to gRPC response
            TransactionResponse response = buildResponse(responseDTO);
            
            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (InsufficientFundsException e) {
            responseObserver.onError(
                io.grpc.Status.FAILED_PRECONDITION
                    .withDescription("Insufficient funds: " + e.getMessage())
                    .asRuntimeException()
            );
        } catch (IneligibleAccountException e) {
            responseObserver.onError(
                io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Ineligible account: " + e.getMessage())
                    .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription("Error processing transaction: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void processDeposit(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            // Convert gRPC request to DTO
            TransactionRequestDTO requestDTO = TransactionRequestDTO.builder()
                    .fromAccount(request.getFromAccount())
                    .toAccount(request.getToAccount())
                    .amount(new BigDecimal(request.getAmount()))
                    .currencyType(CurrencyType.valueOf(request.getCurrencyType()))
                    .description(request.getDescription())
                    .build();
            
            // Process the deposit
            TransactionResponseDTO responseDTO = transactionService.processDeposit(requestDTO);
            
            // Convert to gRPC response
            TransactionResponse response = buildResponse(responseDTO);
            
            // Send response
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (IneligibleAccountException e) {
            responseObserver.onError(
                io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Ineligible account: " + e.getMessage())
                    .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription("Error processing deposit: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    @Override
    public void getTransactionByReference(TransactionReferenceRequest request,
                                          StreamObserver<TransactionResponse> responseObserver) {
        try {
            TransactionResponseDTO dto = transactionService.getTransactionByReference(request.getTransactionReference());
            TransactionResponse response = buildResponse(dto);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error retrieving transaction: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getTransactionsByAccountId(AccountTransactionsRequest request,
                                           StreamObserver<TransactionsListResponse> responseObserver) {
        try {
            List<TransactionResponseDTO> fromAccountTransaction = null;
            List<TransactionResponseDTO> toAccountTransaction = null;

            try {
                fromAccountTransaction = transactionService.getTransactionsByFromAccount(request.getAccountId());
            } catch (TransactionNotFoundException ignored) {
                // No transaction found for from account, that's OK
            }

            try {
                toAccountTransaction = transactionService.getTransactionsByToAccount(request.getAccountId());
            } catch (TransactionNotFoundException ignored) {
                // No transaction found for to account, that's OK
            }

            TransactionsListResponse.Builder responseBuilder = TransactionsListResponse.newBuilder()
                    .setPage(request.getPage())
                    .setSize(request.getSize());

            int count = 0;

            if (fromAccountTransaction != null) {
                for (TransactionResponseDTO dto : fromAccountTransaction) {
                    responseBuilder.addTransactions(buildResponse(dto));
                    count++;
                }
            }

            if (toAccountTransaction != null) {
                for (TransactionResponseDTO dto : toAccountTransaction) {
                    responseBuilder.addTransactions(buildResponse(dto));
                    count++;
                }
            }

            responseBuilder.setTotalCount(count);

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error retrieving transactions: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getTransactionsByFromAccount(AccountRequest request, StreamObserver<TransactionsListResponse> responseObserver) {
        try {
            List<TransactionResponseDTO> transactions = transactionService.getTransactionsByFromAccount(request.getAccountId());

            TransactionsListResponse.Builder responseBuilder = TransactionsListResponse.newBuilder();
            for (TransactionResponseDTO dto : transactions) {
                responseBuilder.addTransactions(buildResponse(dto));
            }
            responseBuilder.setTotalCount(transactions.size());
            // Set page and size if you implement pagination

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("An unexpected error occurred: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getTransactionsByToAccount(AccountRequest request, StreamObserver<TransactionsListResponse> responseObserver) {
        try {
            List<TransactionResponseDTO> transactions = transactionService.getTransactionsByToAccount(request.getAccountId());

            TransactionsListResponse.Builder responseBuilder = TransactionsListResponse.newBuilder();
            for (TransactionResponseDTO dto : transactions) {
                responseBuilder.addTransactions(buildResponse(dto));
            }
            responseBuilder.setTotalCount(transactions.size());
            // Set page and size if you implement pagination

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("An unexpected error occurred: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getTransactionByTransactionTime(TransactionTimeRequest request, StreamObserver<TransactionsListResponse> responseObserver) {
        try {
            // Parse the transaction time from the request string
            LocalDateTime transactionTime = LocalDateTime.parse(request.getTransactionTime(), DATE_FORMATTER);

            // Call the service method to get the transaction
            List<TransactionResponseDTO> transactions = transactionService.getTransactionByTransactionTime(transactionTime);

            TransactionsListResponse.Builder responseBuilder = TransactionsListResponse.newBuilder();

            // Build the response and send it
            for (TransactionResponseDTO dto : transactions) {
                responseBuilder.addTransactions(buildResponse(dto));
            }

            responseBuilder.setTotalCount(transactions.size());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (DateTimeParseException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid date format. Expected format: " + DATE_FORMATTER.toString())
                    .asRuntimeException());
        } catch (TransactionNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Transaction not found for the given transaction time")
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("An unexpected error occurred: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private TransactionResponse buildResponse(TransactionResponseDTO dto) {
    return TransactionResponse.newBuilder()
            .setTransactionReference(dto.getTransactionReference())
            .setFromAccount(dto.getFromAccount())
            .setToAccount(dto.getToAccount())
            .setAmount(dto.getAmount().toString())
            .setCurrencyType(String.valueOf(dto.getCurrencyType()))
            .setTransactionType(TransactionType.valueOf(dto.getTransactionType().toString()))
            .setTransactionStatus(TransactionStatus.valueOf(dto.getTransactionType().toString()))
            .setFeeAmount(dto.getFeeAmount().toString())
            .setFeeCurrency(dto.getFeeCurrencyType().toString())
            .setTimestamp(dto.getTransactionTime() .format(DATE_FORMATTER))
            .setDescription(dto.getDescription())
            .build();
}
}