package mtogo.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mtogo.sql.core.AuthReceiverService;
import mtogo.sql.model.DTO.AuthDTO;
import mtogo.sql.ports.out.IAuthRepository;

@ExtendWith(MockitoExtension.class)
public class AuthReceiverTest {

    @Mock
    IAuthRepository repository;

    @Test
    void returnErrorStatusOnNotfound() {

        AuthReceiverService service = new AuthReceiverService(repository);

        Map<String, Object> result = service.handleAuthLookup("", "");

        assertEquals("not_found", result.get("status"));
    }

    @Test
    void okStatusOnEmailMatch() {

        String email = "test@test.com";
        String service = "customer";

        AuthDTO authDTO = new AuthDTO();
        authDTO.email = email;
        authDTO.passwordHash = "hash";

        when(repository.fetchAuthPerEmail(email)).thenReturn(authDTO);
        when(repository.fetchActorIdForAuth(email, service)).thenReturn("1");

        AuthReceiverService authReceiver = new AuthReceiverService(repository);

        Map<String, Object> result = authReceiver.handleAuthLookup(email, service);

        assertEquals(result.get("status"), "ok");
    }

    @Test
    void okStatusButNegativeActorIDOnRepoError() {

        String email = "test@test.com";
        String service = "customer";

        AuthDTO authDTO = new AuthDTO();
        authDTO.email = email;
        authDTO.passwordHash = "hash";

        when(repository.fetchAuthPerEmail(email)).thenReturn(authDTO);
        when(repository.fetchActorIdForAuth(email, service)).thenReturn("-1"); // to be considered an error..?

        AuthReceiverService authReceiver = new AuthReceiverService(repository);

        Map<String, Object> result = authReceiver.handleAuthLookup(email, service);

        if (!(result.get("user") instanceof Map<?, ?>)) {
            fail("nested map could not be typecasted");
        }
        var nestedResult = (Map<?, ?>) result.get("user");

        assertEquals(result.get("status"), "ok");
        assertEquals(nestedResult.get("actor_id"), "-1");
    }    
    
    @Test
    void okStatusButNegativeActorIDOnNoServiceMatch() {

        String email = "test@test.com";
        String service = "nonexistent";

        AuthDTO authDTO = new AuthDTO();
        authDTO.email = email;
        authDTO.passwordHash = "hash";

        when(repository.fetchAuthPerEmail(email)).thenReturn(authDTO);
        when(repository.fetchActorIdForAuth(email, service)).thenReturn("none");

        AuthReceiverService authReceiver = new AuthReceiverService(repository);

        Map<String, Object> result = authReceiver.handleAuthLookup(email, service);

        if (!(result.get("user") instanceof Map<?, ?>)) {
            fail("nested map could not be typecasted");
        }
        var nestedResult = (Map<?, ?>) result.get("user");

        assertEquals(result.get("status"), "ok");
        assertEquals(nestedResult.get("actor_id"), "none");
    }
}
