package org.transactionMicroservice.dto;

import com.accountMicroservice.grpc.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.transactionMicroservice.model.TransactionDescription;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO {
    
    @NotBlank(message = "From account is required")
    @Size(max = 20, message = "From account must be at most 20 characters")
    private String fromAccount;
    
    @NotBlank(message = "To account is required")
    @Size(max = 20, message = "To account must be at most 20 characters")
    private String toAccount;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    private BigDecimal amount;
    
    @NotNull(message = "Currency type is required")
    private CurrencyType currencyType;
    
    @NotNull(message = "Transaction type is required")
    private TransactionDescription.TransactionType transactionType;
    
    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Size(max = 50, message = "Initiated by must be at most 50 characters")
    private String initiatedBy;
    
    private LocalDateTime valueDate;
}