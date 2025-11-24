/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.supplier.factory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import mtogo.supplier.DTO.LegacyOrder;
import mtogo.supplier.DTO.OrderDetailsDTO;
import mtogo.supplier.DTO.OrderLineDTO;

/**
 *
 * @author kotteletfisk
 */
public class OrderDTOFactory {

    public static OrderDetailsDTO createFromLegacy(LegacyOrder legacy) throws IllegalArgumentException {

        if (legacy == null) {
            throw new IllegalArgumentException("Passed object was null");
        }

        try {
             // FIXME: Risk of collisions!
            UUID id = UUID.randomUUID();

            List<OrderLineDTO> lines = legacy.getOrderlines().stream().map((line) -> {
                return new OrderLineDTO(id, line.getItemId(), line.getUnitPrice(), line.getAmount());
            }).collect(Collectors.toList());

            return new OrderDetailsDTO(id, legacy.getPhone(), OrderDetailsDTO.orderStatus.created, lines);

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
