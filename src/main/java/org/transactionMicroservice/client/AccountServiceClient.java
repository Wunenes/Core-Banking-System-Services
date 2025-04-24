package org.transactionMicroservice.client;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.transactionMicroservice.exceptions.AccountNotFoundException;
import org.transactionMicroservice.exceptions.IneligibleAccountException;
import org.transactionMicroservice.exceptions.InsufficientFundsException;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLException;
import java.io.File;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        if (channel == null || channel.isShutdown()) {
            try {
                SslContext sslContext = GrpcSslContexts.forClient()
                        .trustManager(new File(caCertPath))
                        .keyManager(
                                new File(clientCertPath),
                                new File(clientKeyPath)
                        )
                        .build();
                // Set up channel
                channel = NettyChannelBuilder.forAddress(grpcHost, grpcPort)
                        .sslContext(sslContext)
                        .build();

                // Create initial stub
                blockingStub = AccountServiceGrpc.newBlockingStub(channel);
                checkAndRefreshTokenIfNeeded();
            } catch (SSLException e) {
                Logger.getLogger(AccountServiceClient.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private <T> T executeWithRetry(Supplier<T> grpcCall, String accountNumber) throws Exception {
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
                    throw handleGrpcException(e, accountNumber);
                }
            }
        }

        throw new RuntimeException("Authentication failed after " + MAX_RETRIES + " attempts");
    }

    private Exception handleGrpcException(StatusRuntimeException e, String accountNumber) {
        Status.Code code = e.getStatus().getCode();
        Metadata trailers = e.getTrailers();

        switch (code) {
            case PERMISSION_DENIED:
                String message = trailers != null
                        ? trailers.get(Metadata.Key.of("message", Metadata.ASCII_STRING_MARSHALLER))
                        : "No error message provided.";
                String account = trailers != null
                        ? trailers.get(Metadata.Key.of("account", Metadata.ASCII_STRING_MARSHALLER))
                        : "Unknown account";
                String attemptedOperation = trailers != null
                        ? trailers.get(Metadata.Key.of("attempted operation", Metadata.ASCII_STRING_MARSHALLER))
                        : "unknown operation";
                return new IneligibleAccountException("Account: " + account + " has no permission to " + attemptedOperation + message, accountNumber);

            case NOT_FOUND:
                return new AccountNotFoundException("Account not found: " + e.getStatus().getDescription(), accountNumber);

            case FAILED_PRECONDITION:
                String preAccount = trailers != null
                        ? trailers.get(Metadata.Key.of("account", Metadata.ASCII_STRING_MARSHALLER))
                        : "unknown";
                String balance = trailers != null
                        ? trailers.get(Metadata.Key.of("balance", Metadata.ASCII_STRING_MARSHALLER))
                        : "unknown";
                String requested = trailers != null
                        ? trailers.get(Metadata.Key.of("requested", Metadata.ASCII_STRING_MARSHALLER))
                        : "unknown";
                return new InsufficientFundsException("Insufficient funds: Account " + preAccount +
                        " has balance " + balance + ", tried to debit " + requested);

            default:
                return new RuntimeException("Unhandled gRPC error: " + e.getStatus(), e);
        }
    }

    public CreditResponse creditAccount(String accountNumber, BigDecimal amount, CurrencyType currencyType) throws Exception {
        ensureConnection();
        return executeWithRetry(() -> {
            CreditRequest request = CreditRequest.newBuilder()
                    .setAccountNumber(accountNumber)
                    .setAmount(amount.toString())
                    .setCurrencyType(currencyType)
                    .build();
            return blockingStub.creditAccount(request);
        }, accountNumber);
    }

    public DebitResponse debitAccount(String accountNumber, BigDecimal amount, CurrencyType currencyType) throws Exception {
        ensureConnection();
        return executeWithRetry(() -> {
            DebitRequest request = DebitRequest.newBuilder()
                    .setAccountNumber(accountNumber)
                    .setAmount(amount.toString())
                    .setCurrencyType(currencyType)
                    .build();
            return blockingStub.debitAccount(request);

        }, accountNumber);
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
            // Default expiration if not specified (e.g., 1 hour)
            this.tokenExpirationTime = System.currentTimeMillis() + 3600000;
            logger.info("Token refreshed, no expiration claim found, using default 1 hour");
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
        }
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}