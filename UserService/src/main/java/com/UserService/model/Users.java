package com.UserService.model;

import com.UserService.encryptors.*;
import com.UserService.model.userDescription.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_user_id", columnList = "user_id"),
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_phone_number", columnList = "phone_number"),
                @Index(name = "idx_users_tax_id", columnList = "tax_identification_number"),
                @Index(name = "idx_users_gov_id", columnList = "government_id_number"),
                @Index(name = "idx_users_kyc_status", columnList = "kyc_verified"),
                @Index(name = "idx_users_user_status", columnList = "user_status"),
                @Index(name = "idx_users_risk_category", columnList = "risk_category"),
                @Index(name = "idx_users_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name="user_id", nullable = false)
    @Convert(converter = UUIDEncryptor.class)
    private UUID userId;

    // Basic personal information
    @Column(name = "first_name", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String firstName;

    @Column(name = "middle_name")
    @Convert(converter = StringEncryptor.class)
    private String middleName;

    @Column(name = "last_name", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    @Convert(converter = SearchableStringEncryptor.class)
    private String email;

    @Column(name = "password_hash", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String passwordHash;

    @Column(name = "phone_number", nullable = false, unique = true)
    @Convert(converter = SearchableStringEncryptor.class)
    private String phoneNumber;

    // Address information
    @Column(name = "address_line1", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String addressLine1;

    @Column(name = "address_line2")
    @Convert(converter = StringEncryptor.class)
    private String addressLine2;

    @Column(name = "city", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String city;

    @Column(name = "state", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String state;

    @Column(name = "postal_code", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String postalCode;

    @Column(name = "country", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String country;

    // KYC information
    @Column(name = "date_of_birth", nullable = false)
    @Convert(converter = LocalDateEncryptor.class)
    private LocalDate dateOfBirth;

    @Column(name = "nationality", nullable = false)
    @Convert(converter = StringEncryptor.class)
    private String nationality;

    @Column(name = "tax_identification_number", nullable = false, unique = true)
    @Convert(converter = StringEncryptor.class)
    private String taxIdentificationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "government_id_type", nullable = false)
    @Convert(converter = GovernmentIdTypeEncryptor.class)
    private GovernmentIdType governmentIdType;

    @Column(name = "government_id_number", nullable = false, unique = true)
    @Convert(converter = StringEncryptor.class)
    private String governmentIdNumber;

    @Column(name = "occupation")
    @Convert(converter = StringEncryptor.class)
    private String occupation;

    @Column(name = "employer_name")
    @Convert(converter = StringEncryptor.class)
    private String employerName;

    // User verification data
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "kyc_verified", nullable = false)
    private boolean kycVerified;

    // Risk assessment
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false)
    @Convert(converter = RiskCategoryEncryptor.class)
    private RiskCategory riskCategory;

    // User selfie video/image for verification
    @Lob
    @Column(name = "selfie_image")
    private byte[] selfieImage;

    @Column(name = "selfie_image_content_type")
    private String selfieImageContentType;

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    @Convert(converter = StringEncryptor.class)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // User status
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    @Convert(converter = UserStatusEncryptor.class)
    private UserStatus userStatus;

    // Last login information
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip")
    @Convert(converter = StringEncryptor.class)
    private String lastLoginIp;
}