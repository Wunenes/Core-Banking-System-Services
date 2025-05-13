package com.UserService.dto.response;

import com.AccountService.grpc.AccountResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponseDto {
    private String accountId;
    private String accountType;
    private String currencyType;
    private String currentBalance;
    private String interestRate;
    
    // Convert from gRPC AccountResponse to DTO
    public static AccountResponseDto fromGrpcResponse(AccountResponse grpcResponse) {
        return AccountResponseDto.builder()
                .accountId(grpcResponse.getAccountNumber())
                .accountType(grpcResponse.getAccountType().name())
                .currencyType(grpcResponse.getCurrencyType().name())
                .currentBalance(grpcResponse.getCurrentBalance())
                .interestRate("0.000")
                .build();
    }
}