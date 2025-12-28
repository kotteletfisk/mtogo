package mtogo.auth.server.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.security.interfaces.RSAPublicKey;

/**
 * @author MrJustMeDahl
 * Class tasked with using a public key to verify that a token has not been tampered with.
 */
public class JwtTokenParser {

    private final JWTVerifier verifier;

    /**
     * @author MrJustMeDahl
     * @param publicKey - the public key being used to build the needed algorithm.
     * Constructor - Initializes the class with the public key.
     */
    public JwtTokenParser(RSAPublicKey publicKey) {
        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        this.verifier = JWT.require(algorithm).build();
    }

    /**
     * @author MrJustMeDahl
     * @param token - a JWT as a String.
     * @return DecodedJWT - a validated JWT as an object that includes functionality to read the payload.
     * Method - used for validating an incoming JWT has not been tampered with.
     */
    public DecodedJWT validate(String token) {
        return verifier.verify(token);
    }
}
