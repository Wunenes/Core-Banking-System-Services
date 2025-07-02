package com.UserService.dto.request;

import com.UserService.model.userDescription.GovernmentIdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
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
}