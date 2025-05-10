package com.AccountService.dto.response;

import com.AccountService.model.AccountDescription;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
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
