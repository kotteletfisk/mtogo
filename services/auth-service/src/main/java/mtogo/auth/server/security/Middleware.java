package mtogo.auth.server.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author MrJustMeDahl
 * class that acts a Javalin middleware.
 */

public class Middleware {

    private static final JwtTokenParser parser;
    public static final Logger log = LoggerFactory.getLogger(Middleware.class);

    /**
     * @author MrJustMeDahl
     * The public key is being retrieved and the JwtTokenParser initialized with the class as the middleware does not function without it.
     */
    static {
        try {
            parser = new JwtTokenParser(PublicKeyLoader.loadPublicKey());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    /**
     * @author MrJustMeDahl
     * @param ctx - Javalin request context.
     * Method - part of the middleware which handles access to protected endpoints.
     */
    public static void registerAuth(Context ctx) {
        // Retrieve which roles are allowed on the endpoint - if there is nothing, it is a public route and anyone can access.
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
        // For non public routes authorization header is being looked at and format is verified.
        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.info("Invalid authorization header");
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        // Token is retrieved from the header and validated. When validated the payload is added to the request context.
        String token = header.substring("Bearer ".length());

        DecodedJWT jwt = parser.validate(token);

        ctx.attribute("email", jwt.getClaim("email").asString());
        ctx.attribute("role", jwt.getClaim("role").asList(String.class));
        ctx.attribute("actor_id", jwt.getClaim("actor_id").asString());


        for (String r : (List<String>) ctx.attribute("role")) {
            for (RouteRole role : permittedRoles) {
                if (role.toString().equalsIgnoreCase(r)) {
                    return;
                }
            }
        }
        // If the user has a valid token, but don't have the required role for an endpoint a ForbiddenResponse is thrown to prevent the request from reaching the handler on the endpoint.
        log.info("Not allowed");
        throw new ForbiddenResponse("Not allowed");
    }

}
