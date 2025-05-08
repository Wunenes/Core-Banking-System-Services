package com.usersMicroservice.client;

import com.accountMicroservice.grpc.*;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import io.grpc.*;

import java.util.Date;
import java.util.concurrent.Executor;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.usersMicroservice.exceptions.AccountNotFoundException;
import com.usersMicroservice.exceptions.IneligibleAccountException;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLException;
import java.io.File;
import java.util.concurrent.TimeUnit;
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


    private synchronized void ensureConnection() {
        logger.info("Ensuring connection to auth server...");
        if (channel == null || channel.isShutdown()) {
            logger.info("Channel is up");
            try {
                logger.info("Creating SSL context...");
                SslContext sslContext = null;
                try {
                    sslContext = GrpcSslContexts.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)  // Trusts any certificate
                            .build();

                    logger.info("SSL context created successfully");
                } catch (SSLException e) {
                    logger.log(Level.SEVERE, "Failed to create SSL context", e);
                    throw new RuntimeException("Failed to create SSL context", e);
                }

                // Set up channel
                logger.info("Building channel to " + grpcHost + ":" + grpcPort);
                channel = NettyChannelBuilder.forAddress(grpcHost, grpcPort)
                        .usePlaintext()
                        .maxInboundMessageSize(20 * 1024 * 1024)
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(10, TimeUnit.SECONDS)
                        .enableRetry()
                        .maxRetryAttempts(5)
                        .build();

                logger.info("Channel created");

                new Thread(() -> {
                    try {
                        logger.info("Initial channel state: " + channel.getState(true));
                        Thread.sleep(2000);
                        logger.info("Channel state after 2s: " + channel.getState(true));
                        Thread.sleep(3000);
                        logger.info("Channel state after 5s: " + channel.getState(true));
                    } catch (Exception e) {
                        logger.severe("Error checking channel state: " + e.getMessage());
                    }
                }).start();


                // Create initial stub
                blockingStub = AccountServiceGrpc.newBlockingStub(channel);
                logger.info("Connection to account server established");
                checkAndRefreshTokenIfNeeded();
                logger.info("Connection to auth server established");
            } catch (Exception e) {
                logger.info("SSL exception occurred");
                Logger.getLogger(AccountServiceClient.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private <T> T executeWithRetry(Supplier<T> grpcCall) throws Exception {
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
                    throw handleGrpcException(e);
                }
            }
        }

        throw new RuntimeException("Authentication failed after " + MAX_RETRIES + " attempts");
    }

    private Exception handleGrpcException(StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();

        switch (code) {
            case NOT_FOUND:
                return new AccountNotFoundException("Account not found: " + e.getStatus().getDescription(), "");

            case FAILED_PRECONDITION:
                Metadata metadata = e.getTrailers();
                assert metadata != null;
                String errorType = metadata.get(Metadata.Key.of("error-type", Metadata.ASCII_STRING_MARSHALLER));

                if ("INELIGIBLE_ACCOUNT".equals(errorType)) {
                    String accountId = metadata.get(Metadata.Key.of("account-number", Metadata.ASCII_STRING_MARSHALLER));
                    String accountStatus = metadata.get(Metadata.Key.of("account-status", Metadata.ASCII_STRING_MARSHALLER));
                    String attemptedOperation = metadata.get(Metadata.Key.of("attempted-operation", Metadata.ASCII_STRING_MARSHALLER));

                    return new IneligibleAccountException(accountStatus, accountId, attemptedOperation);
                }

            default:
                return new RuntimeException("Unhandled gRPC error: " + e.getStatus(), e);
        }
    }

    public AccountResponse createAccount(String userId, String amount, CurrencyType currencyType, AccountType accountType) throws Exception {
        logger.info("Setting up connection to auth server...");
        ensureConnection();
        logger.info("Starting connection to account microservice... ");
        return executeWithRetry(() -> {
            CreateAccountRequest request = CreateAccountRequest.newBuilder()
                    .setUserId(userId)
                    .setAccountType(accountType)
                    .setCurrentBalance(amount)
                    .setCurrencyType(currencyType)
                    .setInterestRate("0.0000")
                    .build();
            logger.info("Request finished building");
            return blockingStub.createAccount(request);
        });
    }

    public AccountsListResponse getUserAccounts(String userId) throws Exception {
        ensureConnection();
        return executeWithRetry(() -> {
            GetAccountsByUserIdRequest request = GetAccountsByUserIdRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return blockingStub.getAccountDetailsByUserId(request);
        });
    }

    private void refreshToken() throws Exception {
        jwtToken = this.tokenObtainService.obtainTokenFromAuthServer();
        updateStubWithToken();

        JWT jwt = JWTParser.parse(jwtToken);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        Date expirationTime = claims.getExpirationTime();
        if (expirationTime != null) {
            this.tokenExpirationTime = expirationTime.getTime();
            logger.info("Token refreshed, expires at: " + expirationTime);
        } else {
            // Default expiration if not specified (5 minutes)
            this.tokenExpirationTime = System.currentTimeMillis() + 300000;
            logger.info("Token refreshed, no expiration claim found, using default 5 minutes");
        }
    }

    private void checkAndRefreshTokenIfNeeded() {
        synchronized (tokenLock) {
            long currentTime = System.currentTimeMillis();
            // Refresh if token will expire in the next minute
            if (currentTime > tokenExpirationTime - TOKEN_EXPIRY_BUFFER_MS) {
                try {
                    refreshToken();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to proactively refresh token", e);
                    // We'll continue with the current token and handle any errors during the actual call
                }
            }
        }
    }

    private void updateStubWithToken() {
        // Create a metadata with the JWT token
        Metadata metadata = new Metadata();
        metadata.put(
                Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer " + jwtToken
        );


        // Create JWT call credentials
        CallCredentials callCredentials = new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
                appExecutor.execute(() -> {
                    try {
                        applier.apply(metadata);
                    } catch (Throwable e) {
                        applier.fail(Status.UNAUTHENTICATED.withCause(e));
                    }
                });
            }

        };

        synchronized (this) {
            // Synchronize the update to avoid race condition
            blockingStub = AccountServiceGrpc.newBlockingStub(channel)
                    .withCallCredentials(callCredentials);

            logger.info("Updated stub with token");
        }
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}