package com.Middlewear.dto;

import com.UserService.grpc.AccountsListResponse;

import java.util.List;
import java.util.stream.Collectors;

public class AccountsListResponseDto {
    public List<AccountResponseDto> accounts;

    public static AccountsListResponseDto fromProto(AccountsListResponse response) {
        AccountsListResponseDto dto = new AccountsListResponseDto();
        dto.accounts = response.getAccountsList()
                .stream()
                .map(AccountResponseDto::fromProto)
                .collect(Collectors.toList());
        return dto;
    }
}
