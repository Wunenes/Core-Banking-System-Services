package com.accountMicroservice.grpc;

import com.accountMicroservice.exceptions.AccountNotFoundException;
import com.accountMicroservice.exceptions.IneligibleAccountException;
import com.accountMicroservice.exceptions.InsufficientFundsException;
import com.accountMicroservice.service.AccountService;
import com.accountMicroservice.dto.request.*;
import com.accountMicroservice.model.AccountDescription;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {
    private final AccountService accountService;

    // Common metadata keys
    private static final Metadata.Key<String> ERROR_TYPE_KEY = Metadata.Key.of("error-type", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ACCOUNT_NUMBER_KEY = Metadata.Key.of("account-number", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ACCOUNT_STATUS_KEY = Metadata.Key.of("account-status", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ATTEMPTED_OPERATION_KEY = Metadata.Key.of("attempted-operation", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> BALANCE_KEY = Metadata.Key.of("balance", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> REQUESTED_KEY = Metadata.Key.of("requested", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        try {
            log.info("Request to create account for {} received, {}, {}, {}, {}", request.getUserId(), request.getAccountType(), request.getCurrencyType(), request.getCurrentBalance(), request.getInterestRate());
            AccountCreateRequest createRequest = AccountCreateRequest.builder()
                    .userId(UUID.fromString(request.getUserId()))
                    .accountType(AccountDescription.AccountType.valueOf(String.valueOf(request.getAccountType())))
                    .currencyType(AccountDescription.CurrencyType.valueOf(String.valueOf(request.getCurrencyType())))
                    .currentBalance(new BigDecimal(request.getCurrentBalance()))
                    .interestRate(new BigDecimal(request.getInterestRate()))
                    .build();

            log.info("Account request created: {}", createRequest.toString());
            com.accountMicroservice.dto.response.AccountResponse response = accountService.createAccount(createRequest);
            responseObserver.onNext(convertToGrpcResponse(response));
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGenericError(responseObserver, e);
        }
    }

    @Override
    public void getAccountDetailsByUserId(GetAccountsByUserIdRequest request,
                                          StreamObserver<AccountsListResponse> responseObserver) {
        try {
            // Convert the user ID string to UUID
            UUID userId = UUID.fromString(request.getUserId());

            // Call the service method
            List<com.accountMicroservice.dto.response.AccountResponse> accountResponses =
                    accountService.getAccountDetailsByUserId(userId);

            // Convert to gRPC response
            AccountsListResponse.Builder responseBuilder = AccountsListResponse.newBuilder();

            // Add each account to the response
            for (com.accountMicroservice.dto.response.AccountResponse response : accountResponses) {
                responseBuilder.addAccounts(convertToGrpcResponse(response));
            }

            // Send the response
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (AccountNotFoundException e) {
            handleAccountNotFoundError(responseObserver, e);
        } catch (Exception e) {
            handleGenericError(responseObserver, e);
        }
    }

    @Override
    public void getAccountDetails(GetAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        try {
            com.accountMicroservice.dto.response.AccountResponse response = accountService.getAccountDetails(request.getAccountNumber());
            responseObserver.onNext(convertToGrpcResponse(response));
            responseObserver.onCompleted();
        } catch (AccountNotFoundException e) {
            handleAccountNotFoundError(responseObserver, e);
        } catch (Exception e) {
            handleGenericError(responseObserver, e);
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
        } catch(IneligibleAccountException e) {
            handleIneligibleAccountError(responseObserver, e, "account debit operation");
        } catch (AccountNotFoundException e) {
            handleAccountNotFoundError(responseObserver, e);
        } catch (Exception e) {
            handleGenericError(responseObserver, e);
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
        } catch (InsufficientFundsException e) {
            handleInsufficientFundsError(responseObserver, e);
        } catch(IneligibleAccountException e) {
            handleIneligibleAccountError(responseObserver, e, "account debit operation");
        } catch (AccountNotFoundException e) {
            handleAccountNotFoundError(responseObserver, e);
        } catch (Exception e) {
            handleGenericError(responseObserver, e);
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
        } catch (AccountNotFoundException e) {
            handleAccountNotFoundError(responseObserver, e);
        } catch (Exception e) {
            handleGenericError(responseObserver, e);
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
        } catch (InsufficientFundsException e) {
            handleInsufficientFundsError(responseObserver, e);
        } catch(IneligibleAccountException e) {
            handleIneligibleAccountError(responseObserver, e, "account deletion operation");
        } catch (AccountNotFoundException e) {
            handleAccountNotFoundError(responseObserver, e);
        } catch (Exception e) {
            handleGenericError(responseObserver, e);
        }
    }

    // Common error handlers
    private void handleAccountNotFoundError(StreamObserver<?> responseObserver, AccountNotFoundException e) {
        responseObserver.onError(Status.NOT_FOUND
                .withDescription("Account not found: " + e.getMessage())
                .asRuntimeException());
    }

    private void handleInsufficientFundsError(StreamObserver<?> responseObserver, InsufficientFundsException e) {
        Status status = Status.FAILED_PRECONDITION
                .withDescription("Insufficient funds for account operation");

        Metadata metadata = new Metadata();
        metadata.put(ERROR_TYPE_KEY, "INSUFFICIENT_FUNDS");
        metadata.put(ACCOUNT_NUMBER_KEY, e.getAccountNumber());
        metadata.put(BALANCE_KEY, e.getCurrentBalance().toPlainString());
        metadata.put(REQUESTED_KEY, e.getAttemptedAmount().toPlainString());

        responseObserver.onError(status.asRuntimeException(metadata));
    }

    private void handleIneligibleAccountError(StreamObserver<?> responseObserver, IneligibleAccountException e, String operation) {
        Status status = Status.FAILED_PRECONDITION
                .withDescription("Operation not allowed on this account");

        Metadata metadata = new Metadata();
        metadata.put(ERROR_TYPE_KEY, "INELIGIBLE_ACCOUNT");
        metadata.put(ACCOUNT_STATUS_KEY, e.getMessage());
        metadata.put(ACCOUNT_NUMBER_KEY, e.getAccountId());
        metadata.put(ATTEMPTED_OPERATION_KEY, operation);

        responseObserver.onError(status.asRuntimeException(metadata));
    }

    private void handleGenericError(StreamObserver<?> responseObserver, Exception e) {
        responseObserver.onError(Status.INTERNAL
                .withDescription("Internal error: " + e.getMessage())
                .asRuntimeException());
    }

    // Response converters
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