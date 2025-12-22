package mtogo.sql.core;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mtogo.sql.DTO.AuthDTO;
import mtogo.sql.ports.out.IAuthRepository;

public class AuthReceiverService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final IAuthRepository repo;

    public AuthReceiverService(IAuthRepository repo) {
        this.repo = repo;
    }

    public String handleAuthLookup(String email, String service) {

        AuthDTO auth = repo.fetchAuthPerEmail(email);

        if (auth != null) {
            List<String> roles = repo.fetchRolesForAuth(auth.id);
            if (roles == null || roles.isEmpty()) {
                roles = List.of();
            }
            String actorId = repo.fetchActorIdForAuth(email, service);
            if (actorId.equals("-1")) {
                log.info("SQL error while fetching actor id");
            } else if (actorId.equals("-2")) {
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

            try {
                return mapper.writeValueAsString(resp);
            } catch (JsonProcessingException ex) {
                log.error(ex.getMessage());
            }
        }

        return "{\"status\":\"not_found\"}";
    }
}
