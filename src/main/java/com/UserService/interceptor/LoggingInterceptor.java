package com.UserService.interceptor;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

public class LoggingInterceptor implements ServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String transactionId = UUID.randomUUID().toString();
        String clientId = AuthInterceptor.CLIENT_ID_KEY.get();
        clientId = clientId != null ? clientId : "unknown-client";

        MDC.put("transactionId", transactionId);
        MDC.put("clientId", clientId);
        MDC.put("method", call.getMethodDescriptor().getFullMethodName());

        logger.info("Started gRPC call: {}", call.getMethodDescriptor().getFullMethodName());

        return new SimpleForwardingServerCallListener<>(
                next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        if (status.isOk()) {
                            logger.info("Completed gRPC call successfully");
                        } else {
                            logger.warn("gRPC call failed with status: {}, description: {}",
                                    status.getCode(), status.getDescription());
                        }
                        MDC.clear();
                        super.close(status, trailers);
                    }
                }, headers)) {

            @Override
            public void onMessage(ReqT message) {
                logger.debug("Received request message: {}", message);
                super.onMessage(message);
            }
        };
    }
}