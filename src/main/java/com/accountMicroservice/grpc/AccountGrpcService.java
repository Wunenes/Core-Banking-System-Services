package com.accountMicroservice.grpc;

import com.accountMicroservice.service.AccountService;
import com.accountMicroservice.dto.request.*;
import com.accountMicroservice.model.AccountDescription;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {
    private final AccountService accountService;

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        try {
            AccountCreateRequest createRequest = AccountCreateRequest.builder()
                .userId(UUID.fromString(request.getUserId()))
                .accountType(AccountDescription.AccountType.valueOf(String.valueOf(request.getAccountType())))
                .currencyType(AccountDescription.CurrencyType.valueOf(String.valueOf(request.getCurrencyType())))
                .currentBalance(new BigDecimal(request.getCurrentBalance()))
                .interestRate(new BigDecimal(request.getInterestRate()))
                .build();

            com.accountMicroservice.dto.response.AccountResponse response = accountService.createAccount(createRequest);
            responseObserver.onNext(convertToGrpcResponse(response));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getAccountDetails(GetAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        try {
            com.accountMicroservice.dto.response.AccountResponse response = accountService.getAccountDetails(request.getAccountNumber());
            responseObserver.onNext(convertToGrpcResponse(response));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void creditAccount(CreditRequest request, StreamObserver<CreditResponse> responseObserver) {
        try {
            com.accountMicroservice.dto.request.CreditRequest creditRequest = com.accountMicroservice.dto.request.CreditRequest.builder()
                .accountNumber(request.getAccountNumber())
                .amount(new BigDecimal(request.getAmount()))
                .currencyType(AccountDescription.CurrencyType.valueOf(String.valueOf(request.getCurrencyType())))
                .build();

            com.accountMicroservice.dto.response.CreditResponse response = accountService.creditAccount(creditRequest);
            responseObserver.onNext(convertToGrpcCreditResponse(response));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void debitAccount(DebitRequest request, StreamObserver<DebitResponse> responseObserver) {
        try {
            com.accountMicroservice.dto.request.DebitRequest debitRequest = com.accountMicroservice.dto.request.DebitRequest.builder()
                .accountNumber(request.getAccountNumber())
                .amount(new BigDecimal(request.getAmount()))
                .currencyType(AccountDescription.CurrencyType.valueOf(String.valueOf(request.getCurrencyType())))
                .build();

            com.accountMicroservice.dto.response.DebitResponse response = accountService.debitAccount(debitRequest);
            responseObserver.onNext(convertToGrpcDebitResponse(response));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void freezeAction(FreezeActionRequest request, StreamObserver<FreezeActionResponse> responseObserver) {
        try {
            com.accountMicroservice.dto.request.FreezeActionRequest freezeRequest = com.accountMicroservice.dto.request.FreezeActionRequest.builder()
                .action(request.getAction())
                .accountNumber(request.getAccountNumber())
                .reason(request.getReason())
                .build();

            com.accountMicroservice.dto.response.FreezeActionResponse response = accountService.freezeAction(freezeRequest);
            responseObserver.onNext(convertToGrpcFreezeActionResponse(response));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deleteAccount(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            com.accountMicroservice.dto.request.DeleteRequest deleteRequest = com.accountMicroservice.dto.request.DeleteRequest.builder()
                .accountNumber(request.getAccountNumber())
                .receivingAccountNumber(request.getReceivingAccountNumber())
                .build();

            com.accountMicroservice.dto.response.DeleteResponse response = accountService.deleteAccount(deleteRequest);
            responseObserver.onNext(convertToGrpcDeleteResponse(response));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private AccountResponse convertToGrpcResponse(com.accountMicroservice.dto.response.AccountResponse response) {
        return AccountResponse.newBuilder()
            .setAccountType(convertAccountType(response.getAccountType()))
            .setAccountStatus(convertAccountStatus(response.getAccountStatus()))
            .setCurrentBalance(response.getCurrentBalance().toString())
            .setAvailableBalance(response.getAvailableBalance().toString())
            .setCurrencyType(convertCurrencyType(response.getCurrencyType()))
            .setAccountNumber(response.getAccountNumber())
            .build();
    }

    private CreditResponse convertToGrpcCreditResponse(com.accountMicroservice.dto.response.CreditResponse response) {
        return CreditResponse.newBuilder()
            .setAccountNumber(response.getAccountNumber())
            .setAmount(response.getAmount().toString())
            .setCurrencyType(convertCurrencyType(response.getCurrencyType()))
            .build();
    }

    private DebitResponse convertToGrpcDebitResponse(com.accountMicroservice.dto.response.DebitResponse response) {
        return DebitResponse.newBuilder()
            .setAccountNumber(response.getAccountNumber())
            .setAmount(response.getAmount().toString())
            .setCurrencyType(convertCurrencyType(response.getCurrencyType()))
            .build();
    }

    private FreezeActionResponse convertToGrpcFreezeActionResponse(com.accountMicroservice.dto.response.FreezeActionResponse response) {
        return FreezeActionResponse.newBuilder()
            .setAction(response.getAction())
            .setAccountNumber(response.getAccountNumber())
            .setReason(response.getReason())
            .setTimestamp(String.valueOf(response.getTime()))
            .build();
    }

    private DeleteResponse convertToGrpcDeleteResponse(com.accountMicroservice.dto.response.DeleteResponse response) {
        DeleteResponse.Builder builder = DeleteResponse.newBuilder()
            .setAccountNumber(response.getAccountNumber())
            .setTimestamp(String.valueOf(response.getTime()));

        if (response.getCreditResponse() != null) {
            builder.setCreditResponse(convertToGrpcCreditResponse(response.getCreditResponse()));
        }
        if (response.getDebitResponse() != null) {
            builder.setDebitResponse(convertToGrpcDebitResponse(response.getDebitResponse()));
        }

        return builder.build();
    }

    private AccountType convertAccountType(AccountDescription.AccountType type) {
        return AccountType.valueOf(type.name());
    }

    private AccountStatus convertAccountStatus(AccountDescription.AccountStatus status) {
        return AccountStatus.valueOf(status.name());
    }

    private CurrencyType convertCurrencyType(AccountDescription.CurrencyType type) {
        return CurrencyType.valueOf(type.name());
    }
} 