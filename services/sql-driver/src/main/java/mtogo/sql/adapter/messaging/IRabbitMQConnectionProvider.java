package mtogo.sql.adapter.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;

public interface IRabbitMQConnectionProvider {
    public Connection getConnection() throws IOException, TimeoutException, InterruptedException;
}
