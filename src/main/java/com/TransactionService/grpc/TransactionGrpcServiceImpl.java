package com.TransactionService.grpc;

import com.AccountService.grpc.CurrencyType;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import com.TransactionService.dto.TransactionRequestDTO;
import com.TransactionService.dto.TransactionResponseDTO;
import com.TransactionService.exceptions.IneligibleAccountException;
import com.TransactionService.exceptions.InsufficientFundsException;
import com.TransactionService.service.TransactionService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

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