package com.accountMicroservice.service.impl;

import com.accountMicroservice.dto.request.*;
import com.accountMicroservice.dto.response.*;
import com.accountMicroservice.exceptions.IneligibleAccountException;
import com.accountMicroservice.exceptions.AccountNotFoundException;
import com.accountMicroservice.exceptions.InsufficientFundsException;
import com.accountMicroservice.model.Account;
import com.accountMicroservice.model.AccountDescription;
import com.accountMicroservice.repository.AccountRepository;
import com.accountMicroservice.service.AccountService;
import com.accountMicroservice.utils.AccountNumberGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    AccountRepository accountRepository;
    @Override
    public AccountResponse createAccount(AccountCreateRequest request) {
        if(request.getAccountType() == AccountDescription.AccountType.CHECKING &&
                request.getCurrentBalance().compareTo(BigDecimal.valueOf(200)) < 0){
            request.setAccountStatus(AccountDescription.AccountStatus.INACTIVE);
        } else{
            request.setAccountStatus(AccountDescription.AccountStatus.ACTIVE);
        }

        log.info("Request for {} has been received", request.getCurrentBalance());

        Account account = new Account();
        account.setAccountType(request.getAccountType());
        account.setCurrencyType(request.getCurrencyType());
        account.setUserId(request.getUserId());
        account.setAccountStatus(request.getAccountStatus());
        account.setAvailableBalance(request.getCurrentBalance());
        account.setCurrentBalance(request.getCurrentBalance());
        account.setInterestRate(request.getInterestRate());

        while (true){
            String accountNumber = AccountNumberGenerator.generate(request.getAccountType());
            account.setAccountNumber(accountNumber);
            try{
                accountRepository.save(account);
                break;
            } catch(DuplicateKeyException e){
                // loop again to generate new account number
            }
        }

        return new AccountResponse(account.getAccountType(), account.getAccountStatus(),
                account.getCurrentBalance(), account.getAvailableBalance(), account.getCurrencyType(),
                account.getAccountNumber());
    }

    @SneakyThrows
    @Override
    public List<AccountResponse> getAccountDetailsByUserId(UUID userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.isEmpty()){
            throw new AccountNotFoundException("Account not found", userId.toString(), "userId");
        }

        return accounts.stream()
                .map(account -> {
                    try {
                        return getAccountDetails(account.getAccountNumber());
                    } catch (AccountNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

    }

    @Override
    public AccountResponse getAccountDetails(String accountNumber) throws AccountNotFoundException {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountNotFoundException("Account not found", "account number", accountNumber));

        return new AccountResponse(account.getAccountType(), account.getAccountStatus(),
                account.getCurrentBalance(), account.getAvailableBalance(), account.getCurrencyType(),
                account.getAccountNumber());
    }

    @Override
    public CreditResponse creditAccount(CreditRequest request) throws IneligibleAccountException, AccountNotFoundException {
        String accountNumber = request.getAccountNumber();
        BigDecimal amount = request.getAmount();

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountNotFoundException("Account not found", "account number", accountNumber));

        if (account.getAccountStatus() == AccountDescription.AccountStatus.FROZEN ||
                account.getAccountStatus() == AccountDescription.AccountStatus.CLOSED ||
                account.getAccountStatus() == AccountDescription.AccountStatus.DORMANT ){
            throw new IneligibleAccountException(account.getAccountStatus().toString(), accountNumber, "Credit Account");
        }

        if (request.getCurrencyType() != account.getCurrencyType()){
            amount = amount.multiply(BigDecimal.valueOf(1.50));
        }

        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));

        if (account.getAccountStatus() == AccountDescription.AccountStatus.INACTIVE && amount.compareTo(BigDecimal.valueOf(200.00)) > 0){
            account.setAccountStatus(AccountDescription.AccountStatus.ACTIVE);
        }

        accountRepository.save(account);
        return new CreditResponse(request.getAccountNumber(), request.getAmount(), request.getCurrencyType(), account.getAvailableBalance());
    }

    @Override
    public DebitResponse debitAccount(DebitRequest request) throws IneligibleAccountException, InsufficientFundsException, AccountNotFoundException {
        String accountNumber = request.getAccountNumber();
        BigDecimal amount = request.getAmount();

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountNotFoundException("ACCOUNT NOT FOUND", "account number", accountNumber));

        if (account.getAccountStatus() == AccountDescription.AccountStatus.FROZEN ||
                account.getAccountStatus() == AccountDescription.AccountStatus.CLOSED ||
                account.getAccountStatus() == AccountDescription.AccountStatus.DORMANT ||
                account.getAccountStatus() == AccountDescription.AccountStatus.INACTIVE){
            throw new IneligibleAccountException(account.getAccountStatus().toString(), accountNumber, "Debit Account");
        }

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("INSUFFICIENT FUNDS", accountNumber, amount, account.getCurrentBalance(), account.getCurrencyType().toString());
        }

        if (request.getCurrencyType() != account.getCurrencyType()){
            amount = amount.multiply(BigDecimal.valueOf(1.50)); // Use real forex rates in production
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));

        accountRepository.save(account);
        return new DebitResponse(request.getAccountNumber(), request.getAmount(), request.getCurrencyType(), account.getCurrentBalance());
    }

    @Override
    public DeleteResponse deleteAccount(DeleteRequest request) throws AccountNotFoundException, IneligibleAccountException, InsufficientFundsException {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(()-> new AccountNotFoundException("Account not found", "account number", request.getAccountNumber()));

        String accountNumber = account.getAccountNumber();

        if (account.getAccountStatus() == AccountDescription.AccountStatus.FROZEN){
            throw new IneligibleAccountException(account.getAccountStatus().toString(), accountNumber, "Delete Account");
        }

        if (account.getCurrentBalance().compareTo(BigDecimal.valueOf(0)) > 0){
            Account receivingAccount = accountRepository.findByAccountNumber(request.getReceivingAccountNumber())
                            .orElseThrow(()-> new AccountNotFoundException("Receiving account not found", "account number", request.getReceivingAccountNumber()));

            DebitResponse debitResponse = debitAccount(DebitRequest.builder().accountNumber(accountNumber).amount(account.getAvailableBalance()).currencyType(account.getCurrencyType()).build());
            CreditResponse creditResponse = creditAccount(CreditRequest.builder().accountNumber(receivingAccount.getAccountNumber()).amount(account.getAvailableBalance()).currencyType(account.getCurrencyType()).build());

            account.setAccountStatus(AccountDescription.AccountStatus.CLOSED);
            return new DeleteResponse(accountNumber, LocalDateTime.now(), creditResponse, debitResponse);
        }

        account.setAccountStatus(AccountDescription.AccountStatus.CLOSED);
        return new DeleteResponse(accountNumber, LocalDateTime.now());
    }

    @Override
    public FreezeActionResponse freezeAction(FreezeActionRequest request) throws AccountNotFoundException {
        String action = request.getAction();
        String accountNumber = request.getAccountNumber();
        String reason = request.getReason();

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountNotFoundException("Account not found", "account number", accountNumber));

        if(Objects.equals(action, "freezeAccount")){
            account.setAccountStatus(AccountDescription.AccountStatus.FROZEN);
        } else if (Objects.equals(action, "unfreezeAccount")) {
            account.setAccountStatus(AccountDescription.AccountStatus.ACTIVE);
        }

        return new FreezeActionResponse(action, accountNumber, reason, LocalDateTime.now());
    }
}
