package com.datle.esp.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import com.google.auth.oauth2.TokenVerifier;
import com.google.auth.oauth2.TokenVerifier.VerificationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JwtToken {

    private static final int MAX_TOKEN_LIFETIME_SECONDS = 3600;

    private JwtToken() {}

    public static AccessToken generateJwt(@Nonnull ServiceAccountAuth auth) {
        return generateJwt(auth, /*issuer=*/null);
    }

    public static AccessToken generateJwt(@Nonnull ServiceAccountAuth auth, @Nullable String issuer) {
        ServiceAccountJwtAccessCredentials credentials = auth.credentials();
        if (issuer == null) {
            issuer = credentials.getClientEmail();
        }
        Instant now = Instant.now();
        Instant expiryTime = now.plus(auth.maxTokenLifeTimeSeconds(), ChronoUnit.SECONDS);
        String signedToken = JWT.create().withKeyId(credentials.getPrivateKeyId())
                .withIssuer(issuer)
                .withSubject(credentials.getClientEmail())
                .withAudience(auth.serviceName())
                .withIssuedAt(now)
                .withExpiresAt(expiryTime)
                .sign(Algorithm.RSA256(null, (RSAPrivateKey) credentials.getPrivateKey()));
        return new AccessToken(signedToken, Date.from(expiryTime));
    }

    public static JsonWebSignature verify(@Nonnull ServiceAccountAuth auth, @Nonnull String token)
            throws VerificationException {
        return TokenVerifier.newBuilder()
                .setIssuer(auth.credentials().getClientEmail())
                .setAudience(auth.serviceName())
                .setCertificatesLocation(auth.jwksUri())
                .build()
                .verify(token);
    }

    public record ServiceAccountAuth(@Nonnull ServiceAccountJwtAccessCredentials credentials,
                                     @Nonnull String jwksUri,
                                     @Nonnull String serviceName,
                                     int maxTokenLifeTimeSeconds) {

        public static ServiceAccountAuth fromFile(String serviceAccountPath, String serviceName) throws IOException {
            return fromFile(serviceAccountPath, serviceName, MAX_TOKEN_LIFETIME_SECONDS);
        }

        public static ServiceAccountAuth fromFile(String serviceAccountPath, String serviceName,
                int maxTokenLifeTimeSeconds) throws IOException {
            var credentials = ServiceAccountJwtAccessCredentials.fromStream(new FileInputStream(serviceAccountPath));
            String jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/%s"
                    .formatted(credentials.getClientEmail());
            return new ServiceAccountAuth(credentials, jwksUri, serviceName, maxTokenLifeTimeSeconds);
        }
    }

    public static void main(String[] args) throws Exception {
        String token = """
                eyJraWQiOiIzMmU1NmE2MDdhZjE3YzQ0NGViNTg4MjMwZGM4YjBmY2Y2Yjc2NTM2IiwiYWxnIjoiUlMyNTYiLCJ0eXAiOiJKV1QifQ\
                .eyJpc3MiOiJkYXQtc2VydmljZS1hY2NvdW50QGV4cGVyaW1lbnRhbC0yMjk4MDkuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLCJzd\
                WIiOiJkYXQtc2VydmljZS1hY2NvdW50QGV4cGVyaW1lbnRhbC0yMjk4MDkuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLCJhdWQiOiJ\
                ib29rc3RvcmUyLmVuZHBvaW50cy5leHBlcmltZW50YWwtMjI5ODA5LmNsb3VkLmdvb2ciLCJpYXQiOjE3MzM4MDEzNjUsImV4cCI6M\
                TczMzgwMTQ4NX0.hlff8t0x8mqhpp9hKZQ1VintZqMrM4CozFyr2UQpdeFGspZ5sp-rMEvSty7FUlEQtAp6USQre6F7GBP_MU6IKPa\
                LdNdISovddSZdIsATxRCsioNkSU1TvPFhrE9BKbpkwkFabrPvYU5_A4XXuLnQj-IQiTH79bG8IYSg3GQyc-sLCDoPWVdiQsQCWKN18\
                08x-br-7UYcWnp8D-6TLQOIWaBEx34xE18TeuHq7iwPy_zYsJopGmwOGPVqkhuXkVNuq6nEac2hnE0C7UPD3VNggUyP6eVBosq93Ym\
                VMqlDc82K2tnFNPHGXxD0Xjybc9S2XWn-ROxMQjH8WcBxdpPeyA
                """;
        System.out.println(token);
        String serviceAccountFilePath = "/Users/datle/experimental-229809-32e56a607af1.json"; // dat-service-account
        String serviceName = "bookstore2.endpoints.experimental-229809.cloud.goog";

        ServiceAccountAuth serviceAccountAuth = ServiceAccountAuth.fromFile(serviceAccountFilePath, serviceName, 120);

        try {
            System.out.println(verify(serviceAccountAuth, token));
        } catch (VerificationException e) {
            System.out.println(e.getMessage());
        }

        AccessToken accessToken = generateJwt(serviceAccountAuth);
        JsonWebSignature jws = verify(serviceAccountAuth, accessToken.getTokenValue());
        System.out.println(jws);
    }
}
