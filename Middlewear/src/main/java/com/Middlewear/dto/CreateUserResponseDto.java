package com.Middlewear.dto;

import com.UserService.grpc.CreateUserResponse;

public class CreateUserResponseDto {
    public String userId;
    public String firstName;
    public String lastName;
    public String email;
    public String message;

    public static CreateUserResponseDto fromProto(CreateUserResponse response) {
        CreateUserResponseDto dto = new CreateUserResponseDto();
        dto.userId = response.getUserId();
        dto.firstName = response.getFirstName();
        dto.lastName = response.getLastName();
        dto.email = response.getEmail();
        dto.message = response.getMessage();
        return dto;
    }
}
