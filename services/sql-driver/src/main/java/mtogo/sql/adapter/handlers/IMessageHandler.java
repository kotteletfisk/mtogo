/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package mtogo.sql.adapter.handlers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;

/**
 *
 * @author kotteletfisk
 */
public interface IMessageHandler {
    public void handle(Delivery delivery, Channel channel);
}
