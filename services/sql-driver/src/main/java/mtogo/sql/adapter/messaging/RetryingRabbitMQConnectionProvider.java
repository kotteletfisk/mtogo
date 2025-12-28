package mtogo.sql.adapter.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RetryingRabbitMQConnectionProvider implements IRabbitMQConnectionProvider  {

    private final RabbitMQConfig config;
    private final int maxRetries;
    private final long retryDelayMs;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public RetryingRabbitMQConnectionProvider(
            RabbitMQConfig config,
            int maxRetries,
            long retryDelayMs
    ) {
        this.config = config;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public Connection getConnection()
            throws IOException, TimeoutException, InterruptedException {

        for (int i = 0; i < maxRetries; i++) {
            try {
                return config.getConnectionFactory().newConnection("default-connection");
            } catch (IOException | TimeoutException e) {
                log.warn("RabbitMQ connection failed, retrying ({}/{})", i + 1, maxRetries);
                Thread.sleep(retryDelayMs);
            }
        }

        throw new IOException("Failed to connect to RabbitMQ after retries");
    }
}
