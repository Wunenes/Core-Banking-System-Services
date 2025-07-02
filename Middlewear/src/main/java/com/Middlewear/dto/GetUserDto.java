package com.Middlewear.dto;

import com.UserService.grpc.GetUserResponse;

public class GetUserDto {
    public String userId;
    public String firstName;
    public String middleName;
    public String lastName;
    public String email;
    public String phoneNumber;
    public String addressLine1;
    public String addressLine2;
    public String city;
    public String state;
    public String postalCode;
    public String country;
    public String dateOfBirth;
    public String nationality;
    public String taxId;
    public String governmentIdType;
    public String governmentIdNumber;
    public boolean emailVerified;
    public boolean phoneVerified;
    public boolean kycVerified;
    public String userStatus;
    public String riskCategory;

    public static GetUserDto fromProto(GetUserResponse response) {
        GetUserDto dto = new GetUserDto();
        dto.userId = response.getUserId();
        dto.firstName = response.getFirstName();
        dto.middleName = response.getMiddleName();
        dto.lastName = response.getLastName();
        dto.email = response.getEmail();
        dto.phoneNumber = response.getPhoneNumber();
        dto.addressLine1 = response.getAddressLine1();
        dto.addressLine2 = response.getAddressLine2();
        dto.city = response.getCity();
        dto.state = response.getState();
        dto.postalCode = response.getPostalCode();
        dto.country = response.getCountry();
        dto.dateOfBirth = response.getDateOfBirth();
        dto.nationality = response.getNationality();
        dto.taxId = response.getTaxIdentificationNumber();
        dto.governmentIdType = response.getGovernmentIdType().name();
        dto.governmentIdNumber = response.getGovernmentIdNumber();
        dto.emailVerified = response.getEmailVerified();
        dto.phoneVerified = response.getPhoneVerified();
        dto.kycVerified = response.getKycVerified();
        dto.userStatus = response.getUserStatus().name();
        dto.riskCategory = response.getRiskCategory().name();
        return dto;
    }
}
