/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mtogo.sql.ports.out;

import java.io.IOException;

import mtogo.sql.model.event.CustomerOrderCreationEvent;

/**
 *
 * @author kotteletfisk
 */
public interface IOrderCreationEventProducer {

    public boolean orderCreation(CustomerOrderCreationEvent event) throws IOException;
}
