package com.datle.esp.auth;

import com.datle.esp.auth.JwtToken.ServiceAccountAuth;
import com.google.auth.oauth2.AccessToken;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AuthInterceptor implements ClientInterceptor {

    @Nonnull
    private final AuthOptions authOptions;
    private AccessToken cachedToken;

    public AuthInterceptor(@Nonnull AuthOptions authOptions) {
        this.authOptions = authOptions;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                if (authOptions.apikey() != null) {
                    var apiKeyHeader = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(apiKeyHeader, authOptions.apikey());
                }
                String authToken = getAuthToken();
                if (authToken != null) {
                    var authTokenHeader = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(authTokenHeader, "Bearer %s".formatted(authToken));
                }
                super.start(responseListener, headers);
            }
        };
    }

    private @Nullable String getAuthToken() {
        if (authOptions.serviceAccountAuth() == null) {
            return null;
        }
        // discard current token if it's going to expire soon
        if (cachedToken == null || shouldRefresh(cachedToken)) {
            cachedToken = JwtToken.generateJwt(authOptions.serviceAccountAuth());
        }
        return cachedToken.getTokenValue();
    }

    private static boolean shouldRefresh(@Nonnull AccessToken token) {
        // discard current token if it's going to expire soon
        return Duration.between(Instant.now(), token.getExpirationTime().toInstant()).toSeconds() < 60;
    }

    public record AuthOptions(@Nullable String apikey, @Nullable ServiceAccountAuth serviceAccountAuth) {}
}
