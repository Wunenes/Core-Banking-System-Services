package com.accountMicroservice.dto.request;

import com.accountMicroservice.model.Account;
import com.accountMicroservice.model.AccountDescription;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreateRequest extends Account {
    @Autowired
    AccountDescription accountDescription;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private String accountNumber;

    @NotNull(message = "Initial balance cannot be null")
    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Balance must have up to 2 decimal places")
    private BigDecimal availableBalance;

    private BigDecimal currentBalance = availableBalance;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "CHECKING|SAVINGS|LOAN|FOREIGN",
            message = "Invalid account type. Allowed: CHECKING, SAVINGS, LOAN, FOREIGN")
    private AccountDescription.AccountType accountType;

    @NotBlank(message = "Currency type is required")
    @Pattern(regexp = "KES|USD|EUR|GBP",
            message = "Invalid currency. Allowed: KES, USD, EUR, GBP")
    private AccountDescription.CurrencyType currencyType;


    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "1.0", message = "Interest rate cannot exceed 100%")
    @Digits(integer = 1, fraction = 4, message = "Interest rate must have up to 4 decimal places")
    private BigDecimal interestRate;

    private AccountDescription.AccountStatus accountStatus;
}