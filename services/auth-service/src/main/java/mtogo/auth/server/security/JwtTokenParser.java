package mtogo.auth.server.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.security.interfaces.RSAPublicKey;

public class JwtTokenParser {

    private final JWTVerifier verifier;

    public JwtTokenParser(RSAPublicKey publicKey) {
        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        this.verifier = JWT.require(algorithm).build();
    }

    public DecodedJWT validate(String token) {
        return verifier.verify(token);
    }
}
