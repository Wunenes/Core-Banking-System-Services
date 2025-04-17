package com.account_microservice.dto.response;

import com.account_microservice.model.AccountDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private AccountDescription.AccountType accountType;
    private AccountDescription.AccountStatus accountStatus;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private AccountDescription.CurrencyType currencyType;
    private String accountNumber;

}
