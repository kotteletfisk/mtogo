/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.adapter.out;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.DTO.menuItemDTO;
import mtogo.sql.persistence.SQLConnector;
import mtogo.sql.ports.out.ModelRepository;

/**
 *
 * @author kotteletfisk
 */
public class PostgresModelRepository implements ModelRepository {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private SQLConnector sqlConnector;

    public PostgresModelRepository(SQLConnector sqlConnector) {
        this.sqlConnector = sqlConnector;
    }

    @Override
    public OrderDTO createOrder(OrderDTO order, List<OrderLineDTO> orderLines) {

        try (Connection conn = sqlConnector.getConnection()) {
            sqlConnector.createOrder(order, orderLines, conn);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
        }
        return order;
    }

    @Override
    public OrderDetailsDTO customerEnrichLegacyOrder(LegacyOrderDetailsDTO dto) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<menuItemDTO> getMenuItemsBySupplierId(int supplierId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
