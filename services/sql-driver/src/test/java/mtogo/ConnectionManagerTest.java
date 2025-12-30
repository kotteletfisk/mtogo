package mtogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import mtogo.sql.adapter.messaging.ConnectionManager;
import mtogo.sql.adapter.messaging.IRabbitMQConnectionProvider;
import mtogo.sql.adapter.messaging.RabbitMQConfig;
import mtogo.sql.adapter.messaging.RetryingRabbitMQConnectionProvider;

@ExtendWith(MockitoExtension.class)
public class ConnectionManagerTest {

    @Mock
    IRabbitMQConnectionProvider providerMock;

    @Mock
    RabbitMQConfig config;

    @Mock
    ConnectionFactory factory;

    @Mock
    Connection connection;

    @Test
    void singletonConnection() throws IOException, TimeoutException, InterruptedException {

        when(providerMock.getConnection()).thenReturn(connection);

        ConnectionManager manager = new ConnectionManager(providerMock);

        Connection c1 = manager.getConnection();
        Connection c2 = manager.getConnection();

        assertSame(c1, c2);
    }

    @Test
    void providerPanicOnExhaustedAttemps() throws IOException, TimeoutException {

        when(config.getConnectionFactory()).thenReturn(factory);
        doThrow(TimeoutException.class).when(factory).newConnection(anyString());

        IRabbitMQConnectionProvider provider = new RetryingRabbitMQConnectionProvider(config, 5, 500);

        assertThrows(IOException.class, provider::getConnection);
    }
}
