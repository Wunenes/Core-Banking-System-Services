package com.account_microservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteResponse {
    private String accountNumber;
    private LocalDateTime time;
    private CreditResponse creditResponse;
    private DebitResponse debitResponse;

    public DeleteResponse(String accountNumber, LocalDateTime now) {
        this.accountNumber = accountNumber;
        this.time = now;
    }
}
