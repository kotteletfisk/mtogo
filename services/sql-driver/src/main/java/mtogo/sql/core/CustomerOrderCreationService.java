/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.ports.out.IModelRepository;

/**
 *
 * @author kotteletfisk
 */
public class CustomerOrderCreationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final IModelRepository repo;

    public CustomerOrderCreationService(IModelRepository repo) {
        this.repo = repo;
    }

    public void call(OrderDetailsDTO dto) throws Exception {

        OrderDTO order = new OrderDTO(dto);

        List<OrderLineDTO> orderLines = new ArrayList<>();
        for (OrderLineDTO line : dto.getOrderLineDTOS()) {
            orderLines.add(
                    new OrderLineDTO(
                            line.getOrderLineId(),
                            line.getOrderId(),
                            line.getItemId(),
                            line.getPriceSnapshot(),
                            line.getAmount()));
        }

        log.debug("'" + dto + "'");

        repo.createOrder(order, orderLines);
    }
}
