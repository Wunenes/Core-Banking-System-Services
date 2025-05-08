package com.usersMicroservice.service;

import com.accountMicroservice.grpc.AccountResponse;
import com.accountMicroservice.grpc.AccountsListResponse;
import com.accountMicroservice.grpc.CreateAccountRequest;
import com.usersMicroservice.dto.request.CreateUserRequest;
import com.usersMicroservice.dto.request.DeleteUserRequest;
import com.usersMicroservice.dto.request.GetUserRequest;
import com.usersMicroservice.dto.request.UpdateUserRequest;
import com.usersMicroservice.dto.response.CreateUserResponse;
import com.usersMicroservice.dto.response.DeleteUserResponse;
import com.usersMicroservice.dto.response.GetUserResponse;
import com.usersMicroservice.dto.response.UpdateUserResponse;
import com.usersMicroservice.exceptions.UserNotFoundException;

public interface UsersService {
    CreateUserResponse createUser(CreateUserRequest request);
    GetUserResponse getUser(GetUserRequest request) throws Exception;
    AccountsListResponse getUserAccounts(String Email) throws Exception;
    AccountResponse createAccount (String email, CreateAccountRequest request) throws Exception;
    UpdateUserResponse updateUser(UpdateUserRequest request) throws UserNotFoundException;
    DeleteUserResponse deleteUser(DeleteUserRequest request) throws UserNotFoundException;

}
