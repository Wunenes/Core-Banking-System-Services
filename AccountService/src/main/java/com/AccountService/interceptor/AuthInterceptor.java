package com.AccountService.interceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class AuthInterceptor implements ServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    
    public static final Context.Key<String> CLIENT_ID_KEY = Context.key("client-id");
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
            return new ServerCall.Listener<ReqT>() {};
        }
        
        String token = authHeader.substring(7);
        
        try {
            var jwt = jwtDecoder.decode(token);
            String clientId = jwt.getClaimAsString("client_id");
            
            if (clientId == null) {
                logger.warn("Missing client_id in token");
                call.close(Status.PERMISSION_DENIED.withDescription("Invalid token content"), new Metadata());
                return new ServerCall.Listener<ReqT>() {};
            }
            
            // Check for required roles or scopes here
            String scope = jwt.getClaimAsString("scope");
            logger.info("Client: {}, scope: {}, call : {}", clientId, scope, call.getMethodDescriptor().getFullMethodName());
            if (scope == null || !hasRequiredScope(scope, call.getMethodDescriptor().getFullMethodName())) {
                logger.warn("Insufficient privileges for client: {}", clientId);
                call.close(Status.PERMISSION_DENIED.withDescription("Insufficient privileges"), new Metadata());
                return new ServerCall.Listener<ReqT>() {};
            }
            
            Context ctx = Context.current().withValue(CLIENT_ID_KEY, clientId);
            return Contexts.interceptCall(ctx, call, headers, next);
            
        } catch (JwtException e) {
            logger.warn("Invalid JWT: {}", e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
            return new ServerCall.Listener<ReqT>() {};
        }
    }

    private boolean hasRequiredScope(String scope, String methodName) {
        // Implement method-specific scope checking
        if (methodName.contains("CreateAccount") || methodName.contains("DeleteAccount")) {
            return scope.contains("account:write");
        } else if (methodName.contains("GetAccountDetails") || methodName.contains("GetAccountDetailsByUserId")) {
            return scope.contains("account:read");
        } else if (methodName.contains("CreditAccount") || methodName.contains("DebitAccount")) {
            return scope.contains("account:transaction");
        } else if (methodName.contains("FreezeAction")) {
            return scope.contains("account:admin");
        }
        return false;
    }
}