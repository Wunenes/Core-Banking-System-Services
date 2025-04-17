package com.account_microservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteRequest {
    @NotNull(message = "Account Number Required")
    private String accountNumber;

    private String receivingAccountNumber;
}
