package com.AccountService.service;

import com.AccountService.dto.request.*;
import com.AccountService.dto.response.*;
import com.AccountService.exceptions.IneligibleAccountException;
import com.AccountService.exceptions.AccountNotFoundException;
import com.AccountService.exceptions.InsufficientFundsException;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountResponse createAccount(AccountCreateRequest request);
    AccountResponse getAccountDetails(String accountNumber) throws AccountNotFoundException;
    List<AccountResponse> getAccountDetailsByUserId(UUID userId) throws AccountNotFoundException;
    CreditResponse creditAccount(CreditRequest request) throws IneligibleAccountException, AccountNotFoundException;
    DebitResponse debitAccount(DebitRequest request) throws IneligibleAccountException, InsufficientFundsException, AccountNotFoundException;
    DeleteResponse deleteAccount(DeleteRequest request) throws AccountNotFoundException, IneligibleAccountException, InsufficientFundsException;
    FreezeActionResponse freezeAction(FreezeActionRequest request) throws AccountNotFoundException;
}
