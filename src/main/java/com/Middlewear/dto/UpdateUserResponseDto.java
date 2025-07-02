package com.Middlewear.dto;

import com.UserService.grpc.UpdateUserResponse;

public class UpdateUserResponseDto {
    public String email;
    public String message;
    public boolean updated;

    public static UpdateUserResponseDto fromProto(UpdateUserResponse response) {
        UpdateUserResponseDto dto = new UpdateUserResponseDto();
        dto.email = response.getEmail();
        dto.message = response.getMessage();
        dto.updated = response.getUpdated();
        return dto;
    }
}
