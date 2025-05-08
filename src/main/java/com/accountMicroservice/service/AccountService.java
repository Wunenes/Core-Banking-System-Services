package com.accountMicroservice.service;

import com.accountMicroservice.dto.request.*;
import com.accountMicroservice.dto.response.*;
import com.accountMicroservice.exceptions.IneligibleAccountException;
import com.accountMicroservice.exceptions.AccountNotFoundException;
import com.accountMicroservice.exceptions.InsufficientFundsException;

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
