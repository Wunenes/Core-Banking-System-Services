package com.UserService.service;

import com.AccountService.grpc.AccountResponse;
import com.AccountService.grpc.AccountsListResponse;
import com.AccountService.grpc.CreateAccountRequest;
import com.UserService.dto.request.CreateUserRequest;
import com.UserService.dto.request.DeleteUserRequest;
import com.UserService.dto.request.GetUserRequest;
import com.UserService.dto.request.UpdateUserRequest;
import com.UserService.dto.response.CreateUserResponse;
import com.UserService.dto.response.DeleteUserResponse;
import com.UserService.dto.response.GetUserResponse;
import com.UserService.dto.response.UpdateUserResponse;
import com.UserService.exceptions.UserNotFoundException;
import org.springframework.cache.annotation.Cacheable;

public interface UsersService {
    CreateUserResponse createUser(CreateUserRequest request);
    GetUserResponse getUser(GetUserRequest request) throws Exception;
    @Cacheable("accounts")
    AccountsListResponse getUserAccounts(String Email) throws Exception;
    AccountResponse createAccount (String email, CreateAccountRequest request) throws Exception;
    UpdateUserResponse updateUser(UpdateUserRequest request) throws UserNotFoundException;
    DeleteUserResponse deleteUser(DeleteUserRequest request) throws UserNotFoundException;

}
