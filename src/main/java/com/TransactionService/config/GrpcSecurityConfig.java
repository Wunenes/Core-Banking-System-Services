package com.TransactionService.config;

import com.TransactionService.interceptor.AuthInterceptor;
import com.TransactionService.interceptor.LoggingInterceptor;
import com.TransactionService.interceptor.RateLimitInterceptor;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcSecurityConfig {

    @GrpcGlobalServerInterceptor
    public ServerInterceptor authInterceptor() {
        return new AuthInterceptor();
    }

    @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    @GrpcGlobalServerInterceptor
    public ServerInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor();
    }
}