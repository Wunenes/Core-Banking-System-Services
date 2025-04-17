package com.account_microservice.controller;

import com.account_microservice.dto.request.AccountCreateRequest;
import com.account_microservice.dto.request.CreditRequest;
import com.account_microservice.dto.request.DebitRequest;
import com.account_microservice.dto.request.DeleteRequest;
import com.account_microservice.dto.response.AccountResponse;
import com.account_microservice.dto.response.CreditResponse;
import com.account_microservice.dto.response.DebitResponse;
import com.account_microservice.dto.response.DeleteResponse;
import com.account_microservice.exceptions.AccountNotFoundException;
import com.account_microservice.exceptions.IneligibleAccountException;
import com.account_microservice.exceptions.InsufficientFundsException;
import com.account_microservice.service.impl.AccountServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("api/account")
public class AccountController {
    @Autowired
    AccountServiceImpl accountService;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountCreateRequest request){
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<AccountResponse> getAccountData(@RequestParam String accountNumber) throws AccountNotFoundException {
        AccountResponse response = accountService.getAccountDetails(accountNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credit")
    public ResponseEntity<CreditResponse> creditAccount(@RequestBody CreditRequest request) throws IneligibleAccountException, AccountNotFoundException {
        CreditResponse response = accountService.creditAccount(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<DebitResponse> debitAccount(@RequestBody DebitRequest request) throws IneligibleAccountException, AccountNotFoundException, InsufficientFundsException {
        DebitResponse response = accountService.debitAccount(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<DeleteResponse> deleteAccount(@RequestBody DeleteRequest request) throws IneligibleAccountException, InsufficientFundsException, AccountNotFoundException {
        DeleteResponse response = accountService.deleteAccount(request);
        return ResponseEntity.ok(response);
    }
}
