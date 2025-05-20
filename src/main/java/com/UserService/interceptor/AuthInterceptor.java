package com.UserService.interceptor;

import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class AuthInterceptor implements ServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    
    public static final Context.Key<String> CLIENT_ID_KEY = Context.key("clientId");
    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = 
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    
    @Autowired
    private JwtDecoder jwtDecoder;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, 
            Metadata headers, 
            ServerCallHandler<ReqT, RespT> next) {
        
        String authHeader = headers.get(AUTHORIZATION_METADATA_KEY);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid authorization header");
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid authorization header"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
        
        String token = authHeader.substring(7);
        
        try {
            var jwt = jwtDecoder.decode(token);
            String clientId = jwt.getClaimAsString("client_id");
            
            if (clientId == null) {
                logger.warn("Missing client_id in token");
                call.close(Status.PERMISSION_DENIED.withDescription("Invalid token content"), new Metadata());
                return new ServerCall.Listener<>() {
                };
            }
            
            // Check for required roles or scopes here
            String scope = jwt.getClaimAsString("scope");
            if (scope == null || !hasRequiredScope(scope, call.getMethodDescriptor().getFullMethodName())) {
                logger.warn("Insufficient privileges for client: {}", clientId);
                call.close(Status.PERMISSION_DENIED.withDescription("Insufficient privileges"), new Metadata());
                return new ServerCall.Listener<>() {
                };
            }
            
            Context ctx = Context.current().withValue(CLIENT_ID_KEY, clientId);
            return Contexts.interceptCall(ctx, call, headers, next);
            
        } catch (JwtException e) {
            logger.warn("Invalid JWT: {}", e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
            return new ServerCall.Listener<>() {
            };
        }
    }
    
    private boolean hasRequiredScope(String scope, String methodName) {
        // Implement method-specific scope checking
        if (methodName.contains("CreateUser") || methodName.contains("DeleteUser") || methodName.contains("UpdateUser") || methodName.contains("CreateAccount")) {
            return scope.contains("account:write");
        } else if (methodName.contains("GetUser") || methodName.contains("GetUserAccounts")) {
            return scope.contains("account:read");
        } else if (methodName.contains("creditAccount") || methodName.contains("debitAccount")) {
            return scope.contains("account:transaction");
        } else if (methodName.contains("freezeAction")) {
            return scope.contains("account:admin");
        }
        return false;
    }
}