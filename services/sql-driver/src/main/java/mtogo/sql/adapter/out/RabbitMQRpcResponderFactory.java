package mtogo.sql.adapter.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import mtogo.sql.ports.out.IRpcResponder;
import mtogo.sql.ports.out.IRpcResponderFactory;

public class RabbitMQRpcResponderFactory implements IRpcResponderFactory {

    private final ObjectMapper mapper;
    private final Connection connection;

    public RabbitMQRpcResponderFactory(ObjectMapper mapper, Connection connection) {
        this.mapper = mapper;
        this.connection = connection;
    }

    @Override
    public IRpcResponder create(Delivery delivery) {
        return new RabbitMQRpcResponder(mapper, delivery, connection);
    }
}
