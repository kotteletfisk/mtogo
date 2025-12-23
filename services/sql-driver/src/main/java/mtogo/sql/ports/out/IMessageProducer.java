/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package mtogo.sql.ports.out;

/**
 *
 * @author kotteletfisk
 */
public interface IMessageProducer {

        public boolean publishMessage(String routingKey, String message);

        public boolean publishObject(String routingKey, Object value);
}
