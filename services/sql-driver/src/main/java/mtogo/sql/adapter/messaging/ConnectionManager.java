package mtogo.sql.adapter.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;

public class ConnectionManager {

    private final Connection connection;

    public ConnectionManager(IRabbitMQConnectionProvider provider)
            throws IOException, TimeoutException, InterruptedException {
        this.connection = provider.getConnection();
    }

    public Connection getConnection() {
        return connection;
    }
}
