package com.Middlewear.controller;

import com.Middlewear.client.UserServiceClient;
import com.Middlewear.dto.*;
import com.Middlewear.exceptions.IneligibleAccountException;
import com.Middlewear.exceptions.InsufficientFundsException;
import com.Middlewear.exceptions.UserNotFoundException;
import com.UserService.grpc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserServiceClient userServiceClient;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            CreateUserResponse response = userServiceClient.createUser(
                    request.get("firstName"),
                    request.get("middleName"),
                    request.get("lastName"),
                    request.get("email"),
                    request.get("password"),
                    request.get("phoneNumber"),
                    request.get("addressLine1"),
                    request.get("addressLine2"),
                    request.get("city"),
                    request.get("state"),
                    request.get("postalCode"),
                    request.get("country"),
                    LocalDate.parse(request.get("dateOfBirth")),
                    request.get("nationality"),
                    request.get("taxId"),
                    GovernmentIdType.valueOf(request.get("idType")),
                    request.get("idNumber"),
                    RiskCategory.valueOf(request.get("riskCategory"))
            );
            return ResponseEntity.ok(CreateUserResponseDto.fromProto(response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{email}")
    public ResponseEntity<?> getUser(@PathVariable String email) {
        try {
            GetUserResponse response = userServiceClient.getUser(email);
            return ResponseEntity.ok(GetUserDto.fromProto(response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping("/{email}/accounts")
    public ResponseEntity<?> getUserAccounts(@PathVariable String email) {
        try {
            AccountsListResponse response = userServiceClient.getUserAccounts(email);
            return ResponseEntity.ok(AccountsListResponseDto.fromProto(response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PostMapping("/{email}/accounts")
    public ResponseEntity<?> createAccount(
            @PathVariable String email,
            @RequestBody Map<String, String> request) {
        try {
            AccountResponse response = userServiceClient.createAccount(
                    email,
                    request.get("accountType"),
                    request.get("currency"),
                    request.get("currentBalance")
            );
            return ResponseEntity.ok(AccountResponseDto.fromProto(response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @PutMapping("/{email}")
    public ResponseEntity<?> updateUser(@PathVariable String email, @RequestBody Map<String, String> request) {
        try {
            UpdateUserResponse response = userServiceClient.updateUser(
                    email,
                    request.get("firstName"),
                    request.get("middleName"),
                    request.get("lastName"),
                    request.get("phoneNumber"),
                    request.get("addressLine1"),
                    request.get("addressLine2"),
                    request.get("city"),
                    request.get("state"),
                    request.get("postalCode"),
                    request.get("country")
            );
            return ResponseEntity.ok(UpdateUserResponseDto.fromProto(response));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private ResponseEntity<String> handleException(Exception e) {
        if (e instanceof UserNotFoundException) {
            return ResponseEntity.status(404).body(e.getMessage());
        } else if (e instanceof InsufficientFundsException) {
            return ResponseEntity.status(409).body(e.getMessage());
        } else if (e instanceof IneligibleAccountException) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
        return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
    }
}
