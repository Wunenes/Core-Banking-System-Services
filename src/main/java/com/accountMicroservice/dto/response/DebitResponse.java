package com.accountMicroservice.dto.response;

import com.accountMicroservice.model.AccountDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DebitResponse {
    private String accountNumber;
    private BigDecimal amount;
    private AccountDescription.CurrencyType currencyType;
    private BigDecimal newBalance;
}
