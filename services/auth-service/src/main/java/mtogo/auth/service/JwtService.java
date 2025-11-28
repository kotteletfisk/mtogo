package mtogo.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final Algorithm algorithm;
    private final String issuer = "mtogo-auth";

    public JwtService(String privateKeyPem) {
        RSAPrivateKey privateKey = parsePrivateKey(privateKeyPem);
        this.algorithm = Algorithm.RSA256(null, privateKey);
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        try {
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            log.error("Failed to parse RSA private key: {}", e.getMessage());
            throw new RuntimeException("Invalid private key");
        }
    }

    public String createToken(long userId, String role, String entityType, Long entityId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600);

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(userId))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .withClaim("role", role)
                .withClaim("entity_type", entityType)
                .withClaim("entity_id", entityId)
                .sign(algorithm);
    }
}
