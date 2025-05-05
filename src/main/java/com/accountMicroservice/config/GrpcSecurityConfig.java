package com.accountMicroservice.config;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import com.accountMicroservice.interceptor.AuthInterceptor;
import com.accountMicroservice.interceptor.LoggingInterceptor;
import com.accountMicroservice.interceptor.RateLimitInterceptor;

@Configuration
public class GrpcSecurityConfig {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;


    @GrpcGlobalServerInterceptor
    public ServerInterceptor authInterceptor() {
        return authInterceptor;
    }

    @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return loggingInterceptor;
    }

    @GrpcGlobalServerInterceptor
    public ServerInterceptor rateLimitInterceptor() {
        return rateLimitInterceptor;
    }
}