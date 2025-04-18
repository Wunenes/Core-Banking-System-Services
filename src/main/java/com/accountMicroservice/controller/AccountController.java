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

    @PostMapping("/freeze_actions")
    public ResponseEntity<FreezeActionResponse> freezeAction(@RequestBody FreezeActionRequest request) throws AccountNotFoundException {
        FreezeActionResponse response = accountService.freezeAction(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<DeleteResponse> deleteAccount(@RequestBody DeleteRequest request) throws IneligibleAccountException, InsufficientFundsException, AccountNotFoundException {
        DeleteResponse response = accountService.deleteAccount(request);
        return ResponseEntity.ok(response);
    }
}
