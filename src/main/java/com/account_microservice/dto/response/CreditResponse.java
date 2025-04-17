package com.account_microservice.dto.response;

import com.account_microservice.model.AccountDescription;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreditResponse {
    private String accountNumber;
    private BigDecimal amount;
    private AccountDescription.CurrencyType currencyType;

}
