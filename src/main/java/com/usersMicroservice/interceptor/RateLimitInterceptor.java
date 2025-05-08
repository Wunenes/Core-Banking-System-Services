package com.usersMicroservice.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements ServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    // Use caching like Redis or Caffeine in prod
    private final Map<String, RateLimiter> clientRateLimiters = new ConcurrentHashMap<>();
    private final RateLimiterRegistry rateLimiterRegistry;
    
    public RateLimitInterceptor() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(100) // Default limit
                .timeoutDuration(Duration.ZERO)
                .build();
        
        this.rateLimiterRegistry = RateLimiterRegistry.of(config);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String clientId = AuthInterceptor.CLIENT_ID_KEY.get();
        if (clientId == null) {
            clientId = "anonymous";
        }
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        
        // Get or create rate limiter for this client
        RateLimiter rateLimiter = clientRateLimiters.computeIfAbsent(
                clientId, 
                id -> createRateLimiterForMethod(id, methodName)
        );
        
        // Check if rate limit is exceeded
        if (!rateLimiter.acquirePermission()) {
            logger.warn("Rate limit exceeded for client: {} on method: {}", clientId, methodName);
            call.close(Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
        
        return next.startCall(call, headers);
    }
    
    private RateLimiter createRateLimiterForMethod(String clientId, String methodName) {
        // Configure different rate limits based on method and/or client
        RateLimiterConfig customConfig;
        
        if (methodName.contains("createAccount") || methodName.contains("deleteAccount")) {
            // Lower limits for sensitive operations
            customConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .limitForPeriod(10)
                    .timeoutDuration(Duration.ZERO)
                    .build();
        } else if (methodName.contains("getAccountDetails")) {
            // Higher limits for read operations
            customConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .limitForPeriod(200)
                    .timeoutDuration(Duration.ZERO)
                    .build();
        } else {
            // Default for other operations
            customConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .limitForPeriod(50)
                    .timeoutDuration(Duration.ZERO)
                    .build();
        }
        
        return rateLimiterRegistry.rateLimiter(clientId + "-" + methodName, customConfig);
    }
}