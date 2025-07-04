package com.UserService.dto.response;

import com.UserService.model.userDescription.GovernmentIdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetUserResponse {
    private UUID userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phoneNumber;
    
    // Address information
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    
    // KYC information
    private LocalDate dateOfBirth;
    private String nationality;
    private String taxIdentificationNumber;
    private GovernmentIdType governmentIdType;
    private String governmentIdNumber;
    private boolean isEmailVerified;
    private boolean isPhoneNumberVerified;
    private boolean kycVerified;
    private String userStatus;
    private String riskCategory;
}