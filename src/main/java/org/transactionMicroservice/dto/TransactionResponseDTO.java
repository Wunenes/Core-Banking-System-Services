package org.transactionMicroservice.dto;

import com.accountMicroservice.grpc.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.transactionMicroservice.model.TransactionDescription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    
    private Long id;
    private String transactionReference;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private CurrencyType currencyType;
    private LocalDateTime transactionTime;
    private TransactionDescription.TransactionStatus status;
    private String description;
    private TransactionDescription.TransactionType transactionType;
    private BigDecimal feeAmount;
    private String feeCurrencyType;
    private String initiatedBy;
    private LocalDateTime processingDate;
    private LocalDateTime valueDate;
    private BigDecimal debitBalanceAfterTransaction;
    private BigDecimal creditBalanceAfterTransaction;
    private String rejectionReason;
    private LocalDateTime updatedAt;
}