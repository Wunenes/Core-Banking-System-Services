package com.account_microservice.service;

import com.account_microservice.dto.request.AccountCreateRequest;
import com.account_microservice.dto.request.CreditRequest;
import com.account_microservice.dto.request.DebitRequest;
import com.account_microservice.dto.request.DeleteRequest;
import com.account_microservice.dto.response.AccountResponse;
import com.account_microservice.dto.response.CreditResponse;
import com.account_microservice.dto.response.DebitResponse;
import com.account_microservice.dto.response.DeleteResponse;
import com.account_microservice.exceptions.IneligibleAccountException;
import com.account_microservice.exceptions.AccountNotFoundException;
import com.account_microservice.exceptions.InsufficientFundsException;

public interface AccountService {
    AccountResponse createAccount(AccountCreateRequest request);
    AccountResponse getAccountDetails(String accountNumber) throws AccountNotFoundException;
    CreditResponse creditAccount(CreditRequest request) throws IneligibleAccountException, AccountNotFoundException;
    DebitResponse debitAccount(DebitRequest request) throws IneligibleAccountException, InsufficientFundsException, AccountNotFoundException;
    DeleteResponse deleteAccount(DeleteRequest request) throws AccountNotFoundException, IneligibleAccountException, InsufficientFundsException;
}
