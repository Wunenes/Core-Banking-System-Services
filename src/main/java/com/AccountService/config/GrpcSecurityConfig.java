package com.AccountService.config;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;
import com.AccountService.interceptor.AuthInterceptor;
import com.AccountService.interceptor.LoggingInterceptor;
import com.AccountService.interceptor.RateLimitInterceptor;

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