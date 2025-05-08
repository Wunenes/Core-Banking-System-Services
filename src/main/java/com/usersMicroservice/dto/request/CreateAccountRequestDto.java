package com.usersMicroservice.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequestDto {
    private String accountType;
    private String currencyType;
    private String currentBalance;
    private String interestRate;
    
    // Getters, setters, constructors
}