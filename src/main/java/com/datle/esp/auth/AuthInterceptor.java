package com.datle.esp.auth;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AuthInterceptor implements ClientInterceptor {

    @Nonnull
    private final ServiceAccountJwtAccessCredentials credentials;
    @Nonnull
    private final String serviceName;
    @Nullable
    private final String apiKey;

    public AuthInterceptor(@Nonnull String serviceAccountPath, @Nonnull String serviceName, @Nullable String apiKey)
            throws IOException {
        this.credentials = ServiceAccountJwtAccessCredentials.fromStream(new FileInputStream(serviceAccountPath));
        this.serviceName = serviceName;
        this.apiKey = apiKey;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                if (apiKey != null) {
                    var apiKeyHeader = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(apiKeyHeader, AuthInterceptor.this.apiKey);
                }
                var authTokenHeader = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
                String jwtToken = genJwtToken(AuthInterceptor.this.credentials, AuthInterceptor.this.serviceName);
                headers.put(authTokenHeader, "Bearer %s".formatted(jwtToken));
                super.start(responseListener, headers);
            }
        };
    }

    private static String genJwtToken(ServiceAccountJwtAccessCredentials credentials, String serviceName) {
        // todo: cache the token so that we do not need to re-generate for every call
        return JwtTokenGen.generateJwt(credentials, serviceName);
    }
}
