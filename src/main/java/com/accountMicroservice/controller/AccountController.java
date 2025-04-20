package com.accountMicroservice.controller;

import com.accountMicroservice.dto.request.*;
import com.accountMicroservice.dto.response.*;
import com.accountMicroservice.exceptions.AccountNotFoundException;
import com.accountMicroservice.exceptions.IneligibleAccountException;
import com.accountMicroservice.exceptions.InsufficientFundsException;
import com.accountMicroservice.service.impl.AccountServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    @Autowired
    AccountServiceImpl accountServiceImpl;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountCreateRequest request){
        AccountResponse response = accountServiceImpl.createAccount(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get")
    public ResponseEntity<AccountResponse> getAccountData(@RequestParam String accountNumber) throws AccountNotFoundException {
        AccountResponse response = accountServiceImpl.getAccountDetails(accountNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credit")
    public ResponseEntity<CreditResponse> creditAccount(@RequestBody CreditRequest request) throws IneligibleAccountException, AccountNotFoundException {
        CreditResponse response = accountServiceImpl.creditAccount(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<DebitResponse> debitAccount(@RequestBody DebitRequest request) throws IneligibleAccountException, AccountNotFoundException, InsufficientFundsException {
        DebitResponse response = accountServiceImpl.debitAccount(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/freeze_actions")
    public ResponseEntity<FreezeActionResponse> freezeAction(@RequestBody FreezeActionRequest request) throws AccountNotFoundException {
        FreezeActionResponse response = accountServiceImpl.freezeAction(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<DeleteResponse> deleteAccount(@RequestBody DeleteRequest request) throws IneligibleAccountException, InsufficientFundsException, AccountNotFoundException {
        DeleteResponse response = accountServiceImpl.deleteAccount(request);
        return ResponseEntity.ok(response);
    }
}
