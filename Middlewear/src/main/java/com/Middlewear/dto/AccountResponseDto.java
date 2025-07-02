package com.Middlewear.dto;

import com.UserService.grpc.AccountResponse;

public class AccountResponseDto {
    public String accountNumber;
    public String accountType;
    public String currency;
    public String balance;
    public String status;
    public String interestRate;

    public static AccountResponseDto fromProto(AccountResponse response) {
        AccountResponseDto dto = new AccountResponseDto();
        dto.accountNumber = response.getAccountNumber();
        dto.accountType = response.getAccountType();
        dto.currency = response.getCurrency();
        dto.balance = response.getBalance();
        dto.status = response.getStatus();
        dto.interestRate = response.getInterestRate();
        return dto;
    }
}
