package com.account_microservice.dto.request;

import com.account_microservice.model.AccountDescription;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DebitRequest {
    @NotNull(message = "Sending account required")
    private String accountNumber;

    @NotNull(message = "Amount required")
    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    @Digits(integer = 15, fraction = 2, message = "Balance must have up to 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Currency Type required")
    @Pattern(regexp = "KES|USD|EUR|GBP",
            message = "Invalid currency. Allowed: KES, USD, EUR, GBP")
    private AccountDescription.CurrencyType currencyType;
}
