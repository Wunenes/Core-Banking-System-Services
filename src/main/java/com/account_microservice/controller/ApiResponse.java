package com.account_microservice.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private Meta meta;
    private T data;
    private Error error;
    private Map<String, String> links;

    // Static factory methods for success/error responses
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .meta(Meta.builder()
                        .status("success")
                        .timestamp(Instant.now())
                        .build())
                .data(data)
                .build();
    }

    public static ApiResponse<Object> error(String errorCode, String message) {
        return ApiResponse.builder()
                .meta(Meta.builder()
                        .status("error")
                        .timestamp(Instant.now())
                        .build())
                .error(Error.builder()
                        .code(errorCode)
                        .message(message)
                        .build())
                .build();
    }

    // Nested classes
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        private String requestId;
        private String status; // "success", "pending", "error"
        private Instant timestamp;
        private String idempotencyKey;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private String code;
        private String message;
        private Map<String, Object> details;
    }
}