/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package mtogo.sql.adapter.out;

import java.io.IOException;

import mtogo.sql.event.OrderPersistedEvent;
import mtogo.sql.ports.out.IOrderPersistedEventProducer;

/**
 *
 * @author kotteletfisk
 */
public class RabbitMQOrderPersistedEventProducer implements IOrderPersistedEventProducer {

    RabbitMQEventProducer producer;

    public RabbitMQOrderPersistedEventProducer(RabbitMQEventProducer producer) {
        this.producer = producer;
    }

    @Override
    public boolean orderPersisted(OrderPersistedEvent event) throws IOException  {
        return producer.publishObject("supplier:order_persisted", event.dto());
    }


}
