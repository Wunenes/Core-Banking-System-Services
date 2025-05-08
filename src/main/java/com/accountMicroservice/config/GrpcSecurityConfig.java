package com.accountMicroservice.config;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;
import com.accountMicroservice.interceptor.AuthInterceptor;
import com.accountMicroservice.interceptor.LoggingInterceptor;
import com.accountMicroservice.interceptor.RateLimitInterceptor;

@Configuration
public class GrpcSecurityConfig {
    @GrpcGlobalServerInterceptor
    public ServerInterceptor authInterceptor() {
        return new AuthInterceptor();
    }
}