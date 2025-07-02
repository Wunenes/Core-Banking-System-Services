package com.Middlewear.client;

import com.Middlewear.exceptions.UserNotFoundException;
import com.UserService.grpc.*;
import com.Middlewear.exceptions.IneligibleAccountException;
import com.Middlewear.exceptions.InsufficientFundsException;
import com.UserService.grpc.UserServiceGrpc;
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
import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class UserServiceClient {
    @Autowired
    TokenObtainService tokenObtainService;
    private UserServiceGrpc.UserServiceBlockingStub blockingStub;
    ManagedChannel channel;
    private String jwtToken;
    private static final Logger logger = Logger.getLogger(UserServiceClient.class.getName());
    private volatile long tokenExpirationTime;
    private final Object tokenLock = new Object();
    private static final int MAX_RETRIES = 3;
    private static final int TOKEN_EXPIRY_BUFFER_MS = 60000;
    private static final int KEEP_ALIVE_TIME_SECONDS = 30;
    private static final int KEEP_ALIVE_TIMEOUT_SECONDS = 10;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${grpc.server.host}")
    private String grpcHost;

    @Value("${userService.grpc.server.port}")
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
                blockingStub = UserServiceGrpc.newBlockingStub(channel)
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

    private <T> T executeWithRetry(Supplier<T> grpcCall) throws Exception {
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
                String userId = metadata1.get(Metadata.Key.of("user-id", Metadata.ASCII_STRING_MARSHALLER));
                return new UserNotFoundException("User id: ",userId);

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

    public CreateUserResponse createUser(
            String firstName,
            String middleName,
            String lastName,
            String email,
            String password,
            String phoneNumber,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            LocalDate dateOfBirth,
            String nationality,
            String taxId,
            GovernmentIdType idType,
            String idNumber,
            RiskCategory riskCategory
    ) throws Exception {
        return executeWithRetry(() -> {
            CreateUserRequest request = CreateUserRequest.newBuilder()
                    .setFirstName(firstName)
                    .setMiddleName(middleName)
                    .setLastName(lastName)
                    .setEmail(email)
                    .setPassword(password)
                    .setPhoneNumber(phoneNumber)
                    .setAddressLine1(addressLine1)
                    .setAddressLine2(addressLine2)
                    .setCity(city)
                    .setState(state)
                    .setPostalCode(postalCode)
                    .setCountry(country)
                    .setDateOfBirth(dateOfBirth.toString())
                    .setNationality(nationality)
                    .setTaxIdentificationNumber(taxId)
                    .setGovernmentIdType(idType)
                    .setGovernmentIdNumber(idNumber)
                    .setRiskCategory(riskCategory)
                    .build();

            return blockingStub.createUser(request);
        });
    }

    public GetUserResponse getUser(String email) throws Exception {
        return executeWithRetry(() -> {
            GetUserRequest request = GetUserRequest.newBuilder()
                    .setEmail(email)
                    .build();
            return blockingStub.getUser(request);
        });
    }

    public AccountsListResponse getUserAccounts(String email) throws Exception {
        return executeWithRetry(() -> {
            GetUserAccountsRequest request = GetUserAccountsRequest.newBuilder()
                    .setEmail(email)
                    .build();
            return blockingStub.getUserAccounts(request);
        });
    }

    public AccountResponse createAccount(String email, String accountType, String currency, String currentBalance) throws Exception {
        return executeWithRetry(() -> {
            CreateAccountRequest request = CreateAccountRequest.newBuilder()
                    .setEmail(email)
                    .setAccountType(accountType)
                    .setCurrency(currency)
                    .setCurrentBalance(currentBalance)
                    .build();
            return blockingStub.createAccount(request);
        });
    }

    public UpdateUserResponse updateUser(
            String email,
            String firstName,
            String middleName,
            String lastName,
            String phoneNumber,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country
    ) throws Exception {
        return executeWithRetry(() -> {
            UpdateUserRequest request = UpdateUserRequest.newBuilder()
                    .setEmail(email)
                    .setFirstName(firstName)
                    .setMiddleName(middleName)
                    .setLastName(lastName)
                    .setPhoneNumber(phoneNumber)
                    .setAddressLine1(addressLine1)
                    .setAddressLine2(addressLine2)
                    .setCity(city)
                    .setState(state)
                    .setPostalCode(postalCode)
                    .setCountry(country)
                    .build();

            return blockingStub.updateUser(request);
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