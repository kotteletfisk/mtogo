package mtogo.sql.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import mtogo.sql.DTO.AuthDTO;
import mtogo.sql.persistence.AuthQueries;
import mtogo.sql.persistence.SQLConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class AuthReceiver {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public String handleAuthLookup(String email, String service) {
        try (Connection conn = new SQLConnector().getConnection()) {
            AuthQueries authQueries = new AuthQueries(conn);

            AuthDTO auth = authQueries.fetchAuthPerEmail(email);

            if (auth != null) {
                List<String> roles = authQueries.fetchRolesForAuth(auth.id);
                if(roles == null || roles.isEmpty()){
                    roles = List.of();
                }
                String actorId = authQueries.fetchActorIdForAuth(email, service);
                if(actorId.equals("-1")){
                    log.info("SQL error while fetching actor id");
                } else if (actorId.equals("-2")){
                    log.info("Service requester not recognized");
                    actorId = "none";
                }
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> resp = Map.of(
                        "status", "ok",
                        "user", Map.of(
                                "email", auth.email,
                                "password_hash", auth.passwordHash,
                                "roles", roles,
                                "actor_id", actorId
                        )
                );
                return mapper.writeValueAsString(resp);
            } else {
                return "{\"status\":\"not_found\"}";
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            return "{\"status\":\"error\"}";
        }
    }
}
