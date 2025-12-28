/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package mtogo.sql.adapter.out;

import java.io.IOException;

import mtogo.sql.model.event.CustomerOrderCreationEvent;
import mtogo.sql.ports.out.IOrderCreationEventProducer;

/**
 *
 * @author kotteletfisk
 */
public class RabbitMQOrderCreationEventProducer implements IOrderCreationEventProducer {

    private final RabbitMQEventProducer producer;

    public RabbitMQOrderCreationEventProducer(RabbitMQEventProducer producer) {
        this.producer = producer;
    }

    @Override
    public boolean orderCreation(CustomerOrderCreationEvent event) throws IOException {
       return producer.publishObject("customer:order_creation", event.dto());
    }

}
