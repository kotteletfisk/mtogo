package mtogo.sql.adapter.messaging;

import com.rabbitmq.client.ConnectionFactory;

import mtogo.sql.env.IEnvProvider;

public class RabbitMQConfig {

    private final IEnvProvider env;

    private final ConnectionFactory factory;

    public RabbitMQConfig(IEnvProvider env) {
        this.env = env;
        this.factory = createDefaultFactory();
    }

    public ConnectionFactory getConnectionFactory() {
        return factory;
    }

    private ConnectionFactory createDefaultFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
        factory.setPort(5672);
        factory.setUsername(env.getenv("RABBITMQ_USER"));
        factory.setPassword(env.getenv("RABBITMQ_PASS"));
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);
        factory.setTopologyRecoveryEnabled(true);

        // Heartbeat
        factory.setRequestedHeartbeat(60);

        return factory;
    }
}
