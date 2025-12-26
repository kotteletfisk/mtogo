/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package mtogo.sql.model.event;

import mtogo.sql.model.DTO.OrderDetailsDTO;

/**
 *
 * @author kotteletfisk
 */
public record OrderPersistedEvent(OrderDetailsDTO dto) {

}
