package mtogo.supplier.handlers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

/**
 *
 * @author kotteletfisk
 */
public interface IMessageHandler {
    public void handle(Delivery delivery, Channel channel);
}
