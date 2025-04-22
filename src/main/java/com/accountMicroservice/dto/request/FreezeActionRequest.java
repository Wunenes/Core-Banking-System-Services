package com.accountMicroservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class FreezeActionRequest {
    @NotNull
    @Pattern(regexp = "freezeAccount|unfreezeAccount",
             message = "Invalid action inputted, enter freezeAccount or unfreezeAccount"
    )
    private String action;

    @NotNull
    private String accountNumber;

    @NotNull
    private String reason;

    @NotNull
    private LocalDateTime time;
}
