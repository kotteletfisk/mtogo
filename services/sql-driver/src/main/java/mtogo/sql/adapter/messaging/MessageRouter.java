/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.adapter.messaging;

import java.util.Map;

import mtogo.sql.adapter.handlers.IMessageHandler;

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

    public String[] getBindingKeys() {
        return handlers.keySet().toArray(new String[0]);
    }
}
