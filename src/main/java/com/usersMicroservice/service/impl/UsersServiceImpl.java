package com.usersMicroservice.service.impl;

import com.accountMicroservice.grpc.AccountResponse;
import com.accountMicroservice.grpc.AccountsListResponse;
import com.accountMicroservice.grpc.CreateAccountRequest;
import com.usersMicroservice.client.AccountServiceClient;
import com.usersMicroservice.dto.response.*;
import com.usersMicroservice.dto.request.*;
import com.usersMicroservice.exceptions.UserNotFoundException;
import com.usersMicroservice.model.Users;
import com.usersMicroservice.model.userDescription.RiskCategory;
import com.usersMicroservice.model.userDescription.UserStatus;
import com.usersMicroservice.repository.UsersRepository;
import com.usersMicroservice.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsersServiceImpl implements UsersService {

    private static final Logger logger = LoggerFactory.getLogger(UsersServiceImpl.class);

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        // Create new user entity from request
        Users newUser = new Users();

        // Generate a new UUID for the user
        newUser.setUserId(UUID.randomUUID());

        // Set basic user information
        newUser.setFirstName(request.getFirstName());
        newUser.setMiddleName(request.getMiddleName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setPhoneNumber(request.getPhoneNumber());

        // Set address information
        newUser.setAddressLine1(request.getAddressLine1());
        newUser.setAddressLine2(request.getAddressLine2());
        newUser.setCity(request.getCity());
        newUser.setState(request.getState());
        newUser.setPostalCode(request.getPostalCode());
        newUser.setCountry(request.getCountry());

        // Set KYC information
        newUser.setDateOfBirth(request.getDateOfBirth());
        newUser.setNationality(request.getNationality());
        newUser.setTaxIdentificationNumber(request.getTaxIdentificationNumber());
        newUser.setGovernmentIdType(request.getGovernmentIdType());
        newUser.setGovernmentIdNumber(request.getGovernmentIdNumber());
        newUser.setRiskCategory(RiskCategory.LOW);
        newUser.setUserStatus(UserStatus.ACTIVE);
        newUser.setEmailVerified(true);
        newUser.setPhoneVerified(true);
        newUser.setKycVerified(true);

        // Save the user to the database
        Users savedUser = usersRepository.save(newUser);

        // Create and return response
        return CreateUserResponse.builder()
                .userId(savedUser.getUserId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .message("User created successfully")
                .build();
    }

    @Override
    public GetUserResponse getUser(GetUserRequest request) throws Exception {
        logger.info("User search for {} starting...", request.getEmail());
                Users user = usersRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->new UserNotFoundException("User not found", "email", request.getEmail()));

        // Map user entity to response
        return GetUserResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .addressLine1(user.getAddressLine1())
                .addressLine2(user.getAddressLine2())
                .city(user.getCity())
                .state(user.getState())
                .postalCode(user.getPostalCode())
                .country(user.getCountry())
                .dateOfBirth(user.getDateOfBirth())
                .nationality(user.getNationality())
                .taxIdentificationNumber(user.getTaxIdentificationNumber())
                .governmentIdType(user.getGovernmentIdType())
                .governmentIdNumber(user.getGovernmentIdNumber())
                .kycVerified(user.isKycVerified()) // Assuming this method exists
                .accountStatus(user.getUserStatus().toString()) // Assuming this method exists
                .riskCategory(user.getRiskCategory().toString()) // Assuming this method exists
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountsListResponse getUserAccounts(String email) throws Exception {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() ->new UserNotFoundException("User not found", "email", email));
        return accountServiceClient.getUserAccounts(user.getUserId().toString());
    }

    @Override
    @Transactional
    public AccountResponse createAccount (String email, CreateAccountRequest request) throws Exception {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() ->new UserNotFoundException("User not found", "email", email));

        logger.info("Account creation for {} starting...", user.getUserId());

        return accountServiceClient.createAccount(user.getUserId().toString(), request.getCurrentBalance(), request.getCurrencyType(), request.getAccountType());
    }

    @Override
    @Transactional
    public UpdateUserResponse updateUser(UpdateUserRequest request) throws UserNotFoundException {
        // Find user by userId
        Users user = usersRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->new UserNotFoundException("User not found", "email", request.getEmail()));
        // Update user information if provided in the request
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            user.setMiddleName(request.getMiddleName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // Update address information if provided
        if (request.getAddressLine1() != null) {
            user.setAddressLine1(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null) {
            user.setAddressLine2(request.getAddressLine2());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getState() != null) {
            user.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            user.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }

        // Update KYC information if provided
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getNationality() != null) {
            user.setNationality(request.getNationality());
        }
        if (request.getTaxIdentificationNumber() != null) {
            user.setTaxIdentificationNumber(request.getTaxIdentificationNumber());
        }
        if (request.getGovernmentIdType() != null) {
            user.setGovernmentIdType(request.getGovernmentIdType());
        }
        if (request.getGovernmentIdNumber() != null) {
            user.setGovernmentIdNumber(request.getGovernmentIdNumber());
        }

        // Save updated user
        usersRepository.save(user);

        // Create and return response
        return UpdateUserResponse.builder()
                .email(user.getEmail())
                .message("User updated successfully")
                .updated(true)
                .build();
    }

    @Override
    @Transactional
    public DeleteUserResponse deleteUser(DeleteUserRequest request) throws UserNotFoundException {
        // Find user by userId
        Users user = usersRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->new UserNotFoundException("User not found", "email", request.getEmail()));
        // Delete the user
        usersRepository.delete(user);

        // Create and return response
        return DeleteUserResponse.builder()
                .email(request.getEmail())
                .message("User deleted successfully")
                .deleted(true)
                .build();
    }
}
