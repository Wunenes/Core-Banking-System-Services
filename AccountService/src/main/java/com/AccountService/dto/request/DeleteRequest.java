package com.AccountService.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteRequest {
    @NotNull(message = "Account Number Required")
    private String accountNumber;

    private String receivingAccountNumber;
}
