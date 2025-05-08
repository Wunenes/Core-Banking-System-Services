package com.usersMicroservice.dto.response;

import com.accountMicroservice.grpc.AccountResponse;
import com.accountMicroservice.grpc.AccountsListResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsListResponseDto {
    private List<AccountResponseDto> accounts;
    
    // Convert from gRPC AccountsListResponse to DTO
    public static AccountsListResponseDto fromGrpcResponse(AccountsListResponse grpcResponse) {
        List<AccountResponseDto> accountDtos = grpcResponse.getAccountsList()
                .stream()
                .map(AccountResponseDto::fromGrpcResponse)
                .collect(Collectors.toList());
        
        return AccountsListResponseDto.builder()
                .accounts(accountDtos)
                .build();
    }
}