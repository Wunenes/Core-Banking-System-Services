package com.accountMicroservice.model;

import com.accountMicroservice.encryptors.*;
import com.accountMicroservice.service.EncryptionService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "accounts",
        indexes = {
                @Index(name = "idx_user_id", columnList = "userId"),
                @Index(name = "idx_account_number", columnList = "accountNumber", unique = true),
                @Index(name = "idx_currency_type", columnList = "currencyType"),
        })
public class Account {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id", nullable = false)
        @Convert(converter = UUIDEncryptor.class)
        private UUID userId;

        @Column(name = "account_number", nullable = false, unique = true)
        @Convert(converter = StringEncryptor.class)
        private String accountNumber;

        @Column(name="current_balance", nullable = false)
        @Convert(converter = BigDecimalEncryptor.class)
        private BigDecimal currentBalance;

        @Column(name="available_balance", nullable = false)
        @Convert(converter = BigDecimalEncryptor.class)
        private BigDecimal availableBalance;

        @Enumerated(EnumType.STRING)
        @Column(name = "account_type", nullable = false)
        @Convert(converter = AccountTypeEncryptor.class)
        private AccountDescription.AccountType accountType;

        @Enumerated(EnumType.STRING)
        @Column(name = "account_status", nullable = false)
        @Convert(converter = AccountStatusEncryptor.class)
        private AccountDescription.AccountStatus accountStatus;

        @Enumerated(EnumType.STRING)
        @Column(name = "currency_type", nullable = false)
        @Convert(converter = CurrencyTypeEncryptor.class)
        private AccountDescription.CurrencyType currencyType;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @Column(name="interest_rate")
        @Convert(converter = BigDecimalEncryptor.class)
        private BigDecimal interestRate;
}
