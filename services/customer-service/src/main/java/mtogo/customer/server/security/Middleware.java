package mtogo.customer.server.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import mtogo.customer.server.security.JwtTokenParser;
import mtogo.customer.server.security.PublicKeyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Middleware {

    private static final JwtTokenParser parser;
    public static final Logger log = LoggerFactory.getLogger(Middleware.class);

    static {
        try {
            parser = new JwtTokenParser(PublicKeyLoader.loadPublicKey());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    public static void registerAuth(Context ctx) {
        var permittedRoles = ctx.routeRoles();
        if (permittedRoles == null || permittedRoles.isEmpty()) {
            return;
        }
        /*
        String path = ctx.path();
        log.info("Registering auth");
        if (path.equals("/api/login") || path.equals("/api/health")) {
            log.info("public route");
            return;
        }

         */
        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.info("Invalid authorization header");
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String token = header.substring("Bearer ".length());

        DecodedJWT jwt = parser.validate(token);

        ctx.attribute("email", jwt.getClaim("email").asString());
        ctx.attribute("role", jwt.getClaim("role").asList(String.class));


        for (String r : (List<String>) ctx.attribute("role")) {
            for (RouteRole role : permittedRoles) {
                if (role.toString().equalsIgnoreCase(r)) {
                    return;
                }
            }
        }
        log.info("Not allowed");
        throw new ForbiddenResponse("Not allowed");
    }

}
