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

    public String handleAuthLookup(String email) {
        try (Connection conn = new SQLConnector().getConnection()) {
            AuthQueries authQueries = new AuthQueries(conn);

            AuthDTO auth = authQueries.fetchAuthPerEmail(email);

            if (auth != null) {
                List<String> roles = authQueries.fetchRolesForAuth(auth.id);
                if(roles != null){
                    roles = List.of();
                }
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> resp = Map.of(
                        "status", "ok",
                        "user", Map.of(
                                "email", auth.email,
                                "password_hash", auth.passwordHash,
                                "roles", roles
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
