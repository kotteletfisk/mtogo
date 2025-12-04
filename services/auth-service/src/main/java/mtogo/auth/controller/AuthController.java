package mtogo.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import mtogo.auth.dto.LoginRequest;
import mtogo.auth.dto.LoginResponse;
import mtogo.auth.exceptions.APIException;
import mtogo.auth.messaging.RabbitRpcClient;
import mtogo.auth.service.JwtService;
import mtogo.auth.service.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthController {

    private static AuthController instance;
    public static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final RabbitRpcClient rpcClient;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PasswordService passwordService = new PasswordService();

    private AuthController(RabbitRpcClient rpcClient, JwtService jwtService) {
        this.rpcClient = rpcClient;
        this.jwtService = jwtService;
    }

    public static AuthController getInstance(RabbitRpcClient rpcClient, JwtService jwtService) {
        if (instance == null){
            instance = new AuthController(rpcClient, jwtService);
        }
        return instance;
    }

    public void login(Context ctx) throws APIException {
        try {
            var req = ctx.bodyAsClass(LoginRequest.class);
            if (req == null || req.email == null || req.password == null) {
                throw new APIException(400, "Invalid Login Request - invalid payload");
            }

            var rpcRequest = Map.of(
                    "action", "find_user_by_email",
                    "email", req.email,
                    "service", req.service
            );
            String requestJson = mapper.writeValueAsString(rpcRequest);

            String rpcResponseJson;
            try {
                rpcResponseJson = rpcClient.rpcCall(requestJson, "auth:login");
            } catch (IOException | InterruptedException e) {
                log.info("RabbitMQ connection to sql-driver failed");
                throw new APIException(500, "Server connection error - "  + e.getMessage());
            }

            JsonNode rpcResp = mapper.readTree(rpcResponseJson);
            String status = rpcResp.path("status").asText();
            if (!"ok".equals(status)) {
                throw new APIException(401, "Invalid Login Request - Credentials are invalid");
            }

            JsonNode userNode = rpcResp.path("user");
            if (userNode.isMissingNode()) {
                throw new APIException(401, "Invalid Login Request - User not found");
            }

            String email = userNode.path("email").asText();
            String passwordHash = userNode.path("password_hash").asText(null);
            List<String> roles = new ArrayList<>();
            for (JsonNode r : userNode.withArray("roles")) {
                roles.add(r.asText());
            }
            String actorId = userNode.path("actor_id").asText();


            if (passwordHash == null || !passwordService.verify(req.password, passwordHash)) {
                throw new APIException(401, "Invalid Login Request - Incorrect password");
            }

            String token = jwtService.createToken(email, roles, actorId);

            LoginResponse resp = new LoginResponse();
            resp.token = token;
            resp.expires_in = 3600L;
            ctx.json(resp);

        } catch (APIException e){
            throw e;
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new APIException(500, "Something went wrong on our server. Exception: " + e.getMessage());
        }
    }
}
