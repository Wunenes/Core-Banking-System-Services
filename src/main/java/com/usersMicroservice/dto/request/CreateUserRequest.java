package com.usersMicroservice.dto.request;

import com.usersMicroservice.model.userDescription.GovernmentIdType;
import com.usersMicroservice.model.userDescription.RiskCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotNull(message = "First Name is required")
    private String firstName;

    private String middleName;

    @NotNull(message = "Last Name is required")
    private String lastName;

    @NotNull(message = "Email is required")
    private String email;

    @NotNull(message = "Password is required")
    private String password;

    @NotNull(message = "Phone Number is required")
    private String phoneNumber;

    
    // Address information
    @NotNull(message = "Address Line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotNull(message = "City is required")
    private String city;

    @NotNull(message = "State is required")
    private String state;

    @NotNull(message = "Postal Code is required")
    private String postalCode;

    @NotNull(message = "Country is required")
    private String country;
    
    // KYC information
    @NotNull(message = "Date of Birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Nationality is required")
    private String nationality;

    @NotNull(message = "Tax Identification Number is required")
    private String taxIdentificationNumber;

    @NotNull(message = "Government Id Type is required")
    private GovernmentIdType governmentIdType;

    @NotNull(message = "Government Id Number is required")
    private String governmentIdNumber;

    @NotNull(message = "Risk Category is required")
    private RiskCategory riskCategory;
}