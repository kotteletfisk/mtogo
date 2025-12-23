/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.adapter.out;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderDetailsDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.DTO.menuItemDTO;
import mtogo.sql.persistence.SQLConnector;
import mtogo.sql.ports.out.IModelRepository;

/**
 *
 * @author kotteletfisk
 */
public class PostgresModelRepository implements IModelRepository {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final SQLConnector sqlConnector;

    public PostgresModelRepository(SQLConnector sqlConnector) {
        this.sqlConnector = sqlConnector;
    }

    @Override
    public OrderDTO createOrder(OrderDTO order, List<OrderLineDTO> orderLines) throws SQLException {

        try (Connection conn = sqlConnector.getConnection()) {
            return sqlConnector.createOrder(order, orderLines, conn);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    public OrderDetailsDTO customerEnrichLegacyOrder(LegacyOrderDetailsDTO dto) throws SQLException {
        try (Connection conn = sqlConnector.getConnection()) {
            return sqlConnector.customerEnrichLegacyOrder(dto, conn);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<menuItemDTO> getMenuItemsBySupplierId(int supplierId) throws SQLException {
        try (Connection conn = sqlConnector.getConnection()) {
            return sqlConnector.getMenuItemsBySupplierId(supplierId, conn);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    public boolean healthCheck() throws Exception {

        for (int i = 0; i < 10; i++) {
            try {
                Connection conn = sqlConnector.getConnection();
                if (conn.isValid(2)) {
                    return true;
                }
            } catch (PSQLException e) {
                log.warn("Retrying JDBC connection");
                Thread.sleep(2000);
            }
        }
        throw new SQLException("JDBC Connection Failed");
    }
}
