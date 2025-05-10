package com.AccountService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FreezeActionResponse {
    private String action;
    private String accountNumber;
    private String reason;
    private LocalDateTime time;
}
