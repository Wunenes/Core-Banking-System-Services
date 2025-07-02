package com.UserService.grpc;

import com.AccountService.grpc.AccountType;
import com.AccountService.grpc.CurrencyType;
import com.UserService.exceptions.UserNotFoundException;
import com.UserService.service.UsersService;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@GrpcService
public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceGrpcImpl.class);

    @Autowired
    private UsersService usersService;

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        try {
            // Convert gRPC request to service DTO
            com.UserService.dto.request.CreateUserRequest serviceRequest = convertToServiceCreateRequest(request);
            
            // Call service
            com.UserService.dto.response.CreateUserResponse serviceResponse = usersService.createUser(serviceRequest);
            
            // Convert service response to gRPC response
            CreateUserResponse grpcResponse = CreateUserResponse.newBuilder()
                    .setUserId(serviceResponse.getUserId().toString())
                    .setFirstName(serviceResponse.getFirstName())
                    .setLastName(serviceResponse.getLastName())
                    .setEmail(serviceResponse.getEmail())
                    .setMessage(serviceResponse.getMessage())
                    .build();
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error("Error creating user", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error creating user: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        try {
            // Convert gRPC request to service DTO
            com.UserService.dto.request.GetUserRequest serviceRequest = new com.UserService.dto.request.GetUserRequest();
            serviceRequest.setEmail(request.getEmail());
            
            // Call service
            com.UserService.dto.response.GetUserResponse serviceResponse = usersService.getUser(serviceRequest);
            
            // Convert service response to gRPC response
            GetUserResponse grpcResponse = buildGetUserResponse(serviceResponse);
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
            
        } catch (UserNotFoundException e) {
            logger.error("User not found", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("User not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            logger.error("Error getting user", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting user: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserAccounts(GetUserAccountsRequest request, StreamObserver<AccountsListResponse> responseObserver) {
        try {
            // Call service
            com.AccountService.grpc.AccountsListResponse serviceResponse = usersService.getUserAccounts(request.getEmail());
            
            // Build gRPC response
            AccountsListResponse.Builder responseBuilder = AccountsListResponse.newBuilder();
            
            // Convert accounts
            for (com.AccountService.grpc.AccountResponse serviceAccount : serviceResponse.getAccountsList()) {
                AccountResponse grpcAccount = convertToGrpcAccountResponse(serviceAccount);
                responseBuilder.addAccounts(grpcAccount);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (UserNotFoundException e) {
            logger.error("User not found", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("User not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            logger.error("Error getting user accounts", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting user accounts: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        try {
            // Convert gRPC request to service DTO
            com.AccountService.grpc.CreateAccountRequest serviceRequest = com.AccountService.grpc.CreateAccountRequest.newBuilder()
                    .setAccountType(AccountType.valueOf(request.getAccountType()))
                    .setCurrencyType(CurrencyType.valueOf(request.getCurrency()))
                    .setCurrentBalance(request.getCurrentBalance())
                    .setInterestRate(request.getInterestRate())
                    .build();
            
            // Call service
            com.AccountService.grpc.AccountResponse serviceResponse = usersService.createAccount(request.getEmail(), serviceRequest);
            
            // Convert to gRPC response
            AccountResponse grpcResponse = convertToGrpcAccountResponse(serviceResponse);
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
            
        } catch (UserNotFoundException e) {
            logger.error("User not found", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("User not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            logger.error("Error creating account", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error creating account: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
        try {
            // Convert gRPC request to service DTO
            com.UserService.dto.request.UpdateUserRequest serviceRequest = convertToServiceUpdateRequest(request);
            
            // Call service
            com.UserService.dto.response.UpdateUserResponse serviceResponse = usersService.updateUser(serviceRequest);
            
            // Convert to gRPC response
            UpdateUserResponse grpcResponse = UpdateUserResponse.newBuilder()
                    .setEmail(serviceResponse.getEmail())
                    .setMessage(serviceResponse.getMessage())
                    .build();
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
            
        } catch (UserNotFoundException e) {
            logger.error("User not found", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("User not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            logger.error("Error updating user", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error updating user: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
        try {
            // Convert gRPC request to service DTO
            com.UserService.dto.request.DeleteUserRequest serviceRequest = new com.UserService.dto.request.DeleteUserRequest();
            serviceRequest.setEmail(request.getEmail());
            
            // Call service
            com.UserService.dto.response.DeleteUserResponse serviceResponse = usersService.deleteUser(serviceRequest);
            
            // Convert to gRPC response
            DeleteUserResponse grpcResponse = DeleteUserResponse.newBuilder()
                    .setEmail(serviceResponse.getEmail())
                    .setMessage(serviceResponse.getMessage())
                    .build();
            
            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
            
        } catch (UserNotFoundException e) {
            logger.error("User not found", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("User not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error deleting user: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    // Helper methods for conversions
    private com.UserService.dto.request.CreateUserRequest convertToServiceCreateRequest(CreateUserRequest grpcRequest) {
        com.UserService.dto.request.CreateUserRequest serviceRequest = new com.UserService.dto.request.CreateUserRequest();
        
        serviceRequest.setFirstName(grpcRequest.getFirstName());
        serviceRequest.setMiddleName(grpcRequest.getMiddleName());
        serviceRequest.setLastName(grpcRequest.getLastName());
        serviceRequest.setEmail(grpcRequest.getEmail());
        serviceRequest.setPassword(grpcRequest.getPassword());
        serviceRequest.setPhoneNumber(grpcRequest.getPhoneNumber());
        
        // Address information
        serviceRequest.setAddressLine1(grpcRequest.getAddressLine1());
        serviceRequest.setAddressLine2(grpcRequest.getAddressLine2());
        serviceRequest.setCity(grpcRequest.getCity());
        serviceRequest.setState(grpcRequest.getState());
        serviceRequest.setPostalCode(grpcRequest.getPostalCode());
        serviceRequest.setCountry(grpcRequest.getCountry());
        
        // KYC information
        if (grpcRequest.hasDateOfBirth()) {
            Instant instant = Instant.ofEpochSecond(
                    grpcRequest.getDateOfBirth().getSeconds(),
                    grpcRequest.getDateOfBirth().getNanos()
            );
            serviceRequest.setDateOfBirth(LocalDate.ofInstant(instant, ZoneId.systemDefault()));
        }
        
        serviceRequest.setNationality(grpcRequest.getNationality());
        serviceRequest.setTaxIdentificationNumber(grpcRequest.getTaxIdentificationNumber());
        serviceRequest.setGovernmentIdType(convertToServiceGovernmentIdType(grpcRequest.getGovernmentIdType()));
        serviceRequest.setGovernmentIdNumber(grpcRequest.getGovernmentIdNumber());

        return serviceRequest;
    }

    private com.UserService.dto.request.UpdateUserRequest convertToServiceUpdateRequest(UpdateUserRequest grpcRequest) {
        com.UserService.dto.request.UpdateUserRequest serviceRequest = new com.UserService.dto.request.UpdateUserRequest();
        
        serviceRequest.setEmail(grpcRequest.getEmail());
        serviceRequest.setFirstName(grpcRequest.getFirstName());
        serviceRequest.setMiddleName(grpcRequest.getMiddleName());
        serviceRequest.setLastName(grpcRequest.getLastName());
        serviceRequest.setPhoneNumber(grpcRequest.getPhoneNumber());
        
        // Address information
        serviceRequest.setAddressLine1(grpcRequest.getAddressLine1());
        serviceRequest.setAddressLine2(grpcRequest.getAddressLine2());
        serviceRequest.setCity(grpcRequest.getCity());
        serviceRequest.setState(grpcRequest.getState());
        serviceRequest.setPostalCode(grpcRequest.getPostalCode());
        serviceRequest.setCountry(grpcRequest.getCountry());
        
        return serviceRequest;
    }

    private GetUserResponse buildGetUserResponse(com.UserService.dto.response.GetUserResponse serviceResponse) {
        GetUserResponse.Builder builder = GetUserResponse.newBuilder()
                .setUserId(serviceResponse.getUserId().toString())
                .setFirstName(serviceResponse.getFirstName())
                .setLastName(serviceResponse.getLastName());
        
        // Set optional fields if present
        if (serviceResponse.getMiddleName() != null) {
            builder.setMiddleName(serviceResponse.getMiddleName());
        }
        
        builder.setEmail(serviceResponse.getEmail())
               .setPhoneNumber(serviceResponse.getPhoneNumber())
               .setAddressLine1(serviceResponse.getAddressLine1());
        
        if (serviceResponse.getAddressLine2() != null) {
            builder.setAddressLine2(serviceResponse.getAddressLine2());
        }
        
        builder.setCity(serviceResponse.getCity())
               .setState(serviceResponse.getState())
               .setPostalCode(serviceResponse.getPostalCode())
               .setCountry(serviceResponse.getCountry());
        
        // Convert date of birth to Timestamp
        LocalDate dob = serviceResponse.getDateOfBirth();
        Instant dobInstant = dob.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Timestamp dobTimestamp = Timestamp.newBuilder()
                .setSeconds(dobInstant.getEpochSecond())
                .setNanos(dobInstant.getNano())
                .build();
        builder.setDateOfBirth(dobTimestamp);
        
        builder.setNationality(serviceResponse.getNationality())
               .setTaxIdentificationNumber(serviceResponse.getTaxIdentificationNumber())
               .setGovernmentIdType(convertToGrpcGovernmentIdType(serviceResponse.getGovernmentIdType()))
               .setGovernmentIdNumber(serviceResponse.getGovernmentIdNumber())
               .setEmailVerified(serviceResponse.isEmailVerified())
               .setPhoneVerified(serviceResponse.isPhoneNumberVerified())
               .setKycVerified(serviceResponse.isKycVerified())
               .setUserStatus(convertToGrpcUserStatus(com.UserService.model.userDescription.UserStatus.valueOf(serviceResponse.getUserStatus())))
               .setRiskCategory(convertToGrpcRiskCategory(com.UserService.model.userDescription.RiskCategory.valueOf(serviceResponse.getRiskCategory())));
        
        return builder.build();
    }

    private AccountResponse convertToGrpcAccountResponse(com.AccountService.grpc.AccountResponse serviceAccount) {
        AccountResponse.Builder builder = AccountResponse.newBuilder()
                .setAccountNumber(serviceAccount.getAccountNumber())
                .setAccountType(serviceAccount.getAccountType().toString())
                .setCurrency(serviceAccount.getCurrencyType().toString())
                .setBalance(serviceAccount.getCurrentBalance())
                .setStatus(serviceAccount.getAccountStatus().toString())
                .setInterestRate(serviceAccount.getInterestRate());
        return builder.build();
    }

    // Enum conversion methods
    private GovernmentIdType convertToGrpcGovernmentIdType(com.UserService.model.userDescription.GovernmentIdType type) {
        return switch (type) {
            case PASSPORT -> GovernmentIdType.PASSPORT;
            case DRIVERS_LICENSE -> GovernmentIdType.DRIVERS_LICENSE;
            case ID_CARD -> GovernmentIdType.ID_CARD;
            case SOCIAL_SECURITY -> GovernmentIdType.SOCIAL_SECURITY;
            case RESIDENCE_PERMIT -> GovernmentIdType.RESIDENCE_PERMIT;
        };
    }

    private com.UserService.model.userDescription.GovernmentIdType convertToServiceGovernmentIdType(GovernmentIdType type) {
        return switch (type) {
            case RESIDENCE_PERMIT -> com.UserService.model.userDescription.GovernmentIdType.RESIDENCE_PERMIT;
            case PASSPORT -> com.UserService.model.userDescription.GovernmentIdType.PASSPORT;
            case DRIVERS_LICENSE -> com.UserService.model.userDescription.GovernmentIdType.DRIVERS_LICENSE;
            case ID_CARD -> com.UserService.model.userDescription.GovernmentIdType.ID_CARD;
            case SOCIAL_SECURITY -> com.UserService.model.userDescription.GovernmentIdType.SOCIAL_SECURITY;
            default -> throw new IllegalArgumentException("Invalid government id type: " + type);
        };
    }

    private UserStatus convertToGrpcUserStatus(com.UserService.model.userDescription.UserStatus status) {
        return switch (status) {
            case ACTIVE -> UserStatus.ACTIVE;
            case PENDING -> UserStatus.PENDING;
            case SUSPENDED -> UserStatus.SUSPENDED;
            case BLOCKED -> UserStatus.BLOCKED;
            case UNDER_REVIEW -> UserStatus.UNDER_REVIEW;
        };
    }

    private RiskCategory convertToGrpcRiskCategory(com.UserService.model.userDescription.RiskCategory category) {
        return switch (category) {
            case LOW -> RiskCategory.LOW;
            case MEDIUM -> RiskCategory.MEDIUM;
            case HIGH -> RiskCategory.HIGH;
        };
    }
}