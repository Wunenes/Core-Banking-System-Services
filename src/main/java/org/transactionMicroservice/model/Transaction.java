package org.transactionMicroservice.model;

import com.accountMicroservice.grpc.CurrencyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "transaction",
        indexes = {
                @Index(name = "idx_transaction_from_account", columnList = "fromAccount"),
                @Index(name = "idx_transaction_to_account", columnList = "toAccount"),
                @Index(name = "idx_transaction_time", columnList = "transactionTime"),
                @Index(name = "idx_transaction_status", columnList = "status"),
                @Index(name = "idx_transaction_reference", columnList = "transactionReference", unique = true)
        }
)

public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 36)
    private String transactionReference;

    @Column(name = "fromAccount", nullable = false)
    private String fromAccount;

    @Column(name = "toAccount", nullable = false)
    private String toAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currencyType", length = 3, nullable = false)
    private CurrencyType currencyType;

    @CreationTimestamp
    @Column(name = "transactionTime", nullable = false)
    private LocalDateTime transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionDescription.TransactionStatus status;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "transactionType", nullable = false, length = 30)
    private TransactionDescription.TransactionType transactionType;

    @Column(name = "feeAmount", precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "feeCurrencyType", length = 3)
    private String feeCurrencyType;

    @Column(name = "initiatedBy", length = 50)
    private String initiatedBy;

    @Column(name = "processingDate")
    private LocalDateTime processingDate;

    @Column(name = "valueDate")
    private LocalDateTime valueDate;

    @Column(name = "debitBalanceAfterTransaction", precision = 19, scale = 4)
    private BigDecimal debitBalanceAfterTransaction;

    @Column(name = "creditBalanceAfterTransaction", precision = 19, scale = 4)
    private BigDecimal creditBalanceAfterTransaction;

    @Column(name = "rejectionReason")
    private String rejectionReason;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
