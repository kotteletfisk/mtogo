/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mtogo.sql.ports.out;

import java.util.List;

import mtogo.sql.model.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.model.DTO.OrderDTO;
import mtogo.sql.model.DTO.OrderDetailsDTO;
import mtogo.sql.model.DTO.OrderLineDTO;
import mtogo.sql.model.DTO.menuItemDTO;

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
