package com.UserService.client;

import com.AccountService.grpc.*;
import io.grpc.*;
import java.util.concurrent.*;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import com.UserService.exceptions.AccountNotFoundException;
import com.UserService.exceptions.IneligibleAccountException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@Component
public class AccountServiceClient {
    @Autowired
    TokenObtainService tokenObtainService;
    private AccountServiceGrpc.AccountServiceBlockingStub blockingStub;
    ManagedChannel channel;
    private String jwtToken;
    private static final Logger logger = Logger.getLogger(AccountServiceClient.class.getName());
    private volatile long tokenExpirationTime;
    private final Object tokenLock = new Object();
    private static final int MAX_RETRIES = 3;
    private static final int TOKEN_EXPIRY_BUFFER_MS = 60000;
    private static final int KEEP_ALIVE_TIME_SECONDS = 30;
    private static final int KEEP_ALIVE_TIMEOUT_SECONDS = 10;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${grpc.server.host}")
    private String grpcHost;

    @Value("${grpc.server.port}")
    private int grpcPort;

    @Value("${grpc.ssl.caCertPath}")
    private String caCertPath;

    @Value("${grpc.ssl.clientCertPath}")
    private String clientCertPath;

    @Value("${grpc.ssl.clientKeyPath}")
    private String clientKeyPath;

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
                blockingStub = AccountServiceGrpc.newBlockingStub(channel)
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
                            try {
                                handleGrpcException(e);
                            } catch (Exception ex) {
                                throw new CompletionException(ex);
                            }
                        }
                    }
                }

                throw new RuntimeException("Authentication failed after " + MAX_RETRIES + " attempts");
            }, executor).join();
        }
    }

    private void handleGrpcException(StatusRuntimeException e) throws Exception {
        Status.Code code = e.getStatus().getCode();

        switch (code) {
            case NOT_FOUND:
                throw new AccountNotFoundException("Account not found: " + e.getStatus().getDescription(), "");

            case FAILED_PRECONDITION:
                Metadata metadata = e.getTrailers();
                assert metadata != null;
                String errorType = metadata.get(Metadata.Key.of("error-type", Metadata.ASCII_STRING_MARSHALLER));

                if ("INELIGIBLE_ACCOUNT".equals(errorType)) {
                    String accountId = metadata.get(Metadata.Key.of("account-number", Metadata.ASCII_STRING_MARSHALLER));
                    String accountStatus = metadata.get(Metadata.Key.of("account-status", Metadata.ASCII_STRING_MARSHALLER));
                    String attemptedOperation = metadata.get(Metadata.Key.of("attempted-operation", Metadata.ASCII_STRING_MARSHALLER));

                    throw new IneligibleAccountException(accountStatus, accountId, attemptedOperation);
                }
            default:
                throw new RuntimeException("Unhandled gRPC error: " + e.getStatus(), e);
        }
    }

    public AccountResponse createAccount(String userId, String amount, CurrencyType currencyType, AccountType accountType, String interestRate) {
        return executeWithRetry(() -> {
            CreateAccountRequest request = CreateAccountRequest.newBuilder()
                    .setUserId(userId)
                    .setAccountType(accountType)
                    .setCurrentBalance(amount)
                    .setCurrencyType(currencyType)
                    .setInterestRate("0.0000")
                    .build();
            if (accountType != AccountType.CHECKING) {
                request.newBuilderForType()
                        .setInterestRate(interestRate)
                        .build();
            }
            logger.info("Request finished building");
            return blockingStub.createAccount(request);
        });
    }

    @Cacheable("userAccounts")
    public AccountsListResponse getUserAccounts(String userId) {
        return executeWithRetry(() -> {
            GetAccountsByUserIdRequest request = GetAccountsByUserIdRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return blockingStub.getAccountDetailsByUserId(request);
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