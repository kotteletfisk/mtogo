package mtogo.auth.server.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;

import java.util.List;

public class Middleware {

    private static final JwtTokenParser parser;

    static {
        try {
            parser = new JwtTokenParser(PublicKeyLoader.loadPublicKey());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    public static void registerAuth(Context ctx) {

        String path = ctx.path();

        if (path.equals("/api/login") || path.equals("/api/health")) {
            return;
        }

        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String token = header.substring("Bearer ".length());

        DecodedJWT jwt = parser.validate(token);

        ctx.attribute("email", jwt.getClaim("email").asString());
        ctx.attribute("roles", jwt.getClaim("roles").asList(String.class));

        var permittedRoles = ctx.routeRoles();
        if (permittedRoles == null || permittedRoles.isEmpty()) {
            return;
        }
        for (String r : (List<String>) ctx.attribute("roles")) {
            for (RouteRole role : permittedRoles) {
                if (role.toString().equalsIgnoreCase(r)) {
                    return;
                }
            }
        }
        throw new ForbiddenResponse("Not allowed");
    }

}
