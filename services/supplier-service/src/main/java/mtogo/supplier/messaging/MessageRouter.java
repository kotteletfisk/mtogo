package mtogo.supplier.messaging;

import java.util.Map;

import mtogo.supplier.handlers.IMessageHandler;

/**
 *
 * @author kotteletfisk
 */
public class MessageRouter {

    private final Map<String, IMessageHandler> handlers;

    public MessageRouter(Map<String, IMessageHandler> handlers) {
        this.handlers = handlers;
    }

    public IMessageHandler getMessageHandler(String routingKey) throws IllegalArgumentException {
        IMessageHandler handler = handlers.get(routingKey);

        if (handler == null) {
            throw new IllegalArgumentException("No handler found for routing key: " + routingKey);
        }

        return handler;
    }
}