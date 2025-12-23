/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mtogo.sql.ports.out;

import java.util.List;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.DTO.menuItemDTO;

/**
 *
 * @author kotteletfisk
 */
public interface IModelRepository {

    public OrderDTO createOrder(OrderDTO orderDTO, List<OrderLineDTO> orderLineDTOS) throws Exception;

    public OrderDetailsDTO customerEnrichLegacyOrder(LegacyOrderDetailsDTO dto) throws Exception;

    public List<menuItemDTO> getMenuItemsBySupplierId(int supplierId) throws Exception;

    public boolean healthCheck() throws Exception;
}
