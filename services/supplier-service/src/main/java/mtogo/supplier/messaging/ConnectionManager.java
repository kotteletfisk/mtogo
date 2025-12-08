package mtogo.supplier.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ConnectionManager {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static ConnectionManager instance;
    private final Connection connection;

    private ConnectionManager() throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory factory = createDefaultFactory();
        this.connection = getConnectionOrRetry(factory);
    }

    public static synchronized ConnectionManager getConnectionManager() throws IOException, TimeoutException, InterruptedException {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return this.connection;
    }

    // messagequeue might not be ready for connection accept on deploy, so we retry
    // n times or crash
    private Connection getConnectionOrRetry(ConnectionFactory connectionFactory) throws InterruptedException, IOException {

        for (int i = 0; i < 10; i++) {
            try {
                Connection conn = connectionFactory.newConnection("default-connection");
                return conn;
            } catch (IOException | TimeoutException e) {
                log.warn("Retrying rabbitmq connection");
                Thread.sleep(2000);
            }
        }
        throw new IOException("Failed connection to RabbitMQ");
    }

    private ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
        factory.setPort(5672);
        factory.setUsername(System.getenv("RABBITMQ_USER"));
        factory.setPassword(System.getenv("RABBITMQ_PASS"));
        factory.setAutomaticRecoveryEnabled(true);
        return factory;
    }
}
