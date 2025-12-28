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
import java.util.List;

/**
 * @author MrJustMeDahl
 * Class tasked with creating Json web tokens.
 */
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final Algorithm algorithm;
    private final String issuer = "mtogo-auth";

    /**
     * @author MrJustMeDahl
     * @param privateKeyPem - the private key being used to sign tokens with in a String pem format.
     * Constructor - Being used to initialize the service with the private key it needs for signing the tokens.
     */
    public JwtService(String privateKeyPem) {
        RSAPrivateKey privateKey = parsePrivateKey(privateKeyPem);
        this.algorithm = Algorithm.RSA256(null, privateKey);
    }

    /**
     * @author MrJustMeDahl
     * @param pem - the private key as a String in pem format.
     * @return An RSAPrivateKey.
     * Method - used to parse the private key from pem format to the class RSAPrivateKey.
     */
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

    /**
     * @author MrJustMeDahl
     * @param email - Auth identifier, being added to the token as the subject.
     * @param roles - List of which system roles the auth has.
     * @param actorId - The entity ID of the auth, an internal reference to their identity.
     * @return The JWT as a String.
     * Method - used for creating and signing the JWT token with a payload, identifying an auth and their roles.
     */
    public String createToken(String email, List<String> roles,  String actorId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(3600);

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(String.valueOf(email))
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .withClaim("role", roles)
                .withClaim("actor_id", actorId)
                .sign(algorithm);
    }
}
