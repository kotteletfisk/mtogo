package mtogo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mtogo.sql.adapter.persistence.SQLConnector;
import mtogo.sql.env.IEnvProvider;

@ExtendWith(MockitoExtension.class)
public class SQLConnectorTest {

    @Mock
    IEnvProvider env;

    @Test
    void ThrowExceptionOnNoEnv() {

        when(env.getenv(anyString())).thenReturn(null);

        SQLConnector connector = new SQLConnector(env);

        assertThrows(SQLException.class, connector::getConnection);
    }    
    
    @Test
    void ThrowExceptionOnWrongCredentials() {

        when(env.getenv("POSTGRES_DB")).thenReturn("mtogo");
        when(env.getenv("POSTGRES_USER")).thenReturn("wrong-user");
        when(env.getenv("POSTGRES_PASSWORD")).thenReturn("wrong-pass");

        SQLConnector connector = new SQLConnector(env);

        assertThrows(SQLException.class, connector::getConnection);
    }

    
}
