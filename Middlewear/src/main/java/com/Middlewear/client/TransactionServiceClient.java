package com.Middlewear.client;

import com.Middlewear.exceptions.TransactionNotFoundException;
import com.TransactionService.grpc.*;
import com.Middlewear.exceptions.AccountNotFoundException;
import com.Middlewear.exceptions.IneligibleAccountException;
import com.Middlewear.exceptions.InsufficientFundsException;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class TransactionServiceClient {
    @Autowired
    TokenObtainService tokenObtainService;
    private TransactionServiceGrpc.TransactionServiceBlockingStub blockingStub;
    ManagedChannel channel;
    private String jwtToken;
    private static final Logger logger = Logger.getLogger(TransactionServiceClient.class.getName());
    private volatile long tokenExpirationTime;
    private final Object tokenLock = new Object();
    private static final int MAX_RETRIES = 3;
    private static final int TOKEN_EXPIRY_BUFFER_MS = 60000;
    private static final int KEEP_ALIVE_TIME_SECONDS = 30;
    private static final int KEEP_ALIVE_TIMEOUT_SECONDS = 10;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${grpc.server.host}")
    private String grpcHost;

    @Value("${transactionService.grpc.server.port}")
    private int grpcPort;

    @PostConstruct
    public void init() {
        establishConnection();
        // Schedule token refresh before expiration
        scheduler.scheduleAtFixedRate(this::proactiveTokenRefresh, 1, 1, TimeUnit.MINUTES);
    }

    private void proactiveTokenRefresh() {
        try {
            long currentTime = System.currentTimeMillis();
            if (tokenExpirationTime - currentTime < TOKEN_EXPIRY_BUFFER_MS * 3) {
                synchronized (tokenLock) {
                    refreshToken();
                    updateStubWithToken();
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to proactively refresh token: " + e.getMessage());
        }
    }

    private synchronized void establishConnection() {
        try {
            if (channel == null || channel.isShutdown()) {
                channel = NettyChannelBuilder.forAddress(grpcHost, grpcPort)
                        .usePlaintext()
                        .keepAliveTime(KEEP_ALIVE_TIME_SECONDS, TimeUnit.SECONDS)
                        .keepAliveTimeout(KEEP_ALIVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .maxInboundMessageSize(20 * 1024 * 1024) // 20MB
                        .build();

                // Get initial token and create stub
                refreshToken();
                blockingStub = TransactionServiceGrpc.newBlockingStub(channel)
                        .withCallCredentials(new JwtCredential(jwtToken));

                logger.info("gRPC connection established to " + grpcHost + ":" + grpcPort);
            }
        } catch (Exception e) {
            logger.severe("Failed to establish gRPC connection: " + e.getMessage());
            throw new RuntimeException("Failed to establish gRPC connection", e);
        }

    }

    private static class JwtCredential extends CallCredentials {
        private final String jwt;

        JwtCredential(String jwt) {
            this.jwt = jwt;
        }

        @Override
        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
            appExecutor.execute(() -> {
                try {
                    Metadata metadata = new Metadata();
                    metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + jwt);
                    applier.apply(metadata);
                } catch (Throwable e) {
                    applier.fail(Status.UNAUTHENTICATED.withDescription("JWT token error").withCause(e));
                }
            });
        }
    }

    private <T> T executeWithRetry(Supplier<T> grpcCall) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return CompletableFuture.supplyAsync(() -> {
                for (int retry = 0; retry < MAX_RETRIES; retry++) {
                    try {
                        return grpcCall.get();

                    } catch (StatusRuntimeException e) {
                        Status.Code code = e.getStatus().getCode();

                        if (code == Status.Code.UNAUTHENTICATED) {
                            logger.warning("Authentication failed on attempt " + (retry + 1) + ": " + e.getMessage());

                            synchronized (tokenLock) {
                                tokenExpirationTime = 0;
                                try {
                                    refreshToken();
                                    logger.info("Token refreshed, retrying...");
                                } catch (Exception refreshEx) {
                                    logger.log(Level.SEVERE, "Token refresh failed", refreshEx);
                                    throw new RuntimeException("Token refresh failed", refreshEx);
                                }
                            }
                        } else {
                            Exception grpcEx = handleGrpcException(e);
                            if (grpcEx instanceof RuntimeException) {
                                throw (RuntimeException) grpcEx;
                            } else {
                                throw new CompletionException(grpcEx);
                            }
                        }
                    }
                }

                throw new RuntimeException("Authentication failed after " + MAX_RETRIES + " attempts");
            }, executor).join();
        }
    }

    private Exception handleGrpcException(StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();

        switch (code) {
            case NOT_FOUND:
                Metadata metadata1 = e.getTrailers();
                assert  metadata1 != null;
                String errorType1 = metadata1.get(Metadata.Key.of("error-type", Metadata.ASCII_STRING_MARSHALLER));

                if ("ACCOUNT_NOT_FOUND".equals(errorType1)){
                    String accountNumber = metadata1.get(Metadata.Key.of("account-number", Metadata.ASCII_STRING_MARSHALLER));
                    return new AccountNotFoundException("Account not found", "account number: ", accountNumber);
                }

                else if ("TRANSACTION_NOT_FOUND".equals(errorType1)){
                    String transactionReference = metadata1.get(Metadata.Key.of("transaction-reference", Metadata.ASCII_STRING_MARSHALLER));
                    return new TransactionNotFoundException("Transaction number: ", transactionReference);                }


            case FAILED_PRECONDITION:
                Metadata metadata = e.getTrailers();
                assert metadata != null;
                String errorType = metadata.get(Metadata.Key.of("error-type", Metadata.ASCII_STRING_MARSHALLER));

                if ("INSUFFICIENT_BALANCE".equals(errorType)) {
                    String balance = metadata.get(Metadata.Key.of("balance", Metadata.ASCII_STRING_MARSHALLER));
                    String requested = metadata.get(Metadata.Key.of("attempted-operation", Metadata.ASCII_STRING_MARSHALLER));
                    String accountId = metadata.get(Metadata.Key.of("account-number", Metadata.ASCII_STRING_MARSHALLER));

                    return new InsufficientFundsException("Insufficient funds: Account " + accountId +
                            " has balance " + balance + ", tried to debit " + requested);

                } else if ("INELIGIBLE_ACCOUNT".equals(errorType)) {
                    String accountId = metadata.get(Metadata.Key.of("account-number", Metadata.ASCII_STRING_MARSHALLER));
                    String accountStatus = metadata.get(Metadata.Key.of("account-status", Metadata.ASCII_STRING_MARSHALLER));
                    String attemptedOperation = metadata.get(Metadata.Key.of("attempted-operation", Metadata.ASCII_STRING_MARSHALLER));

                    return new IneligibleAccountException(accountStatus, accountId, attemptedOperation);
                }

            default:
                return new RuntimeException("Unhandled gRPC error: " + e.getStatus(), e);
        }
    }

    public TransactionResponse internalTransfer(String fromAccount, String toAccount, String amount, String currencyType, String transactionType, String description, String initiatedBy) throws Exception {
        return executeWithRetry(() -> {
            TransactionRequest request = TransactionRequest.newBuilder()
                    .setFromAccount(fromAccount)
                    .setToAccount(toAccount)
                    .setAmount(amount)
                    .setCurrencyType(currencyType)
                    .setTransactionType(transactionType)
                    .setDescription(description)
                    .setInitiatedBy(initiatedBy)
                    .build();
            return blockingStub.internalTransfer(request);
        });
    }

    public TransactionResponse processDeposit(String fromAccount, String toAccount, String amount, String currencyType, String description) throws Exception {
        return executeWithRetry(() -> {
            TransactionRequest request = TransactionRequest.newBuilder()
                    .setFromAccount(fromAccount)
                    .setToAccount(toAccount)
                    .setAmount(amount)
                    .setCurrencyType(currencyType)
                    .setDescription(description)
                    .build();
            return blockingStub.processDeposit(request);
        });
    }

    public TransactionResponse getTransactionByReference(String transactionReference) throws Exception {
        return executeWithRetry(() -> {
            TransactionReferenceRequest request = TransactionReferenceRequest.newBuilder()
                    .setTransactionReference(transactionReference)
                    .build();
            return blockingStub.getTransactionByReference(request);
        });
    }

    public TransactionsListResponse getTransactionsByAccountId(String accountId, int page, int size) throws Exception {
        return executeWithRetry(() -> {
            AccountTransactionsRequest request = AccountTransactionsRequest.newBuilder()
                    .setAccountId(accountId)
                    .setPage(page)
                    .setSize(size)
                    .build();
            return blockingStub.getTransactionsByAccountId(request);
        });
    }

    public TransactionsListResponse getTransactionsByFromAccount(String accountId) throws Exception {
        return executeWithRetry(() -> {
            AccountRequest request = AccountRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();
            return blockingStub.getTransactionsByFromAccount(request);
        });
    }

    public TransactionsListResponse getTransactionsByToAccount(String accountId) throws Exception {
        return executeWithRetry(() -> {
            AccountRequest request = AccountRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();
            return blockingStub.getTransactionsByToAccount(request);
        });
    }

    public TransactionsListResponse getTransactionByTransactionTime(String transactionTime) throws Exception {
        return executeWithRetry(() -> {
            TransactionTimeRequest request = TransactionTimeRequest.newBuilder()
                    .setTransactionTime(transactionTime)
                    .build();
            return blockingStub.getTransactionByTransactionTime(request);
        });
    }

    private void refreshToken() {
        jwtToken = tokenObtainService.obtainTokenFromAuthServer();
        // Extract expiration from token or get it from the service
        tokenExpirationTime = System.currentTimeMillis() + TOKEN_EXPIRY_BUFFER_MS * 5; // Assuming token lasts 5 minutes
        logger.info("JWT token refreshed, expires in " + (tokenExpirationTime - System.currentTimeMillis()) + "ms");
    }

    private void updateStubWithToken() {
        if (blockingStub != null && jwtToken != null) {
            blockingStub = blockingStub.withCallCredentials(new JwtCredential(jwtToken));
        }
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}