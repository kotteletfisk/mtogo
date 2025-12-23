package mtogo.sql.ports.out;

import com.rabbitmq.client.Delivery;

public interface IRpcResponderFactory {
        IRpcResponder create(Delivery delivery);
}
