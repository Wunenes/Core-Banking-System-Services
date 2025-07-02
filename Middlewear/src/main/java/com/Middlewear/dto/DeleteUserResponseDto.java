package com.Middlewear.dto;

import com.UserService.grpc.DeleteUserResponse;

public class DeleteUserResponseDto {
    public String userId;
    public String email;
    public String message;

    public static DeleteUserResponseDto fromProto(DeleteUserResponse response) {
        DeleteUserResponseDto dto = new DeleteUserResponseDto();
        dto.userId = response.getUserId();
        dto.email = response.getEmail();
        dto.message = response.getMessage();
        return dto;
    }
}
