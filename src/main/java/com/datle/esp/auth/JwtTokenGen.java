package com.datle.esp.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JwtTokenGen {

    private JwtTokenGen() {

    }

    public static @Nonnull String generateJwt(@Nonnull String serviceAccountFile, @Nonnull String serviceName)
            throws IOException {
        return generateJwt(serviceAccountFile, serviceName, null);
    }

    public static @Nonnull String generateJwt(@Nonnull String serviceAccountFile, @Nonnull String serviceName,
            @Nullable String issuer) throws IOException {
        InputStream in = Files.newInputStream(Paths.get(serviceAccountFile));
        ServiceAccountJwtAccessCredentials credentials = ServiceAccountJwtAccessCredentials.fromStream(in);
        return generateJwt(credentials, serviceName, issuer);
    }

    public static String generateJwt(@Nonnull ServiceAccountJwtAccessCredentials credentials,
            @Nonnull String serviceName) {
        return generateJwt(credentials, serviceName, null);
    }

    public static String generateJwt(@Nonnull ServiceAccountJwtAccessCredentials credentials,
            @Nonnull String serviceName, @Nullable String issuer) {
        if (issuer == null) {
            issuer = credentials.getClientEmail();
        }
        Instant now = Instant.now();
        return JWT.create().withKeyId(credentials.getPrivateKeyId())
                .withIssuer(issuer)
                .withSubject(credentials.getClientEmail())
                .withAudience(serviceName)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(1, ChronoUnit.HOURS))
                .sign(Algorithm.RSA256(null, (RSAPrivateKey) credentials.getPrivateKey()));
    }
}
