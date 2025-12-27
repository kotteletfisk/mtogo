/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mtogo.sql.adapter.out;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.adapter.persistence.IPostgresConnectionSupplier;
import mtogo.sql.model.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.model.DTO.OrderDTO;
import mtogo.sql.model.DTO.OrderDetailsDTO;
import mtogo.sql.model.DTO.OrderLineDTO;
import mtogo.sql.model.DTO.menuItemDTO;
import mtogo.sql.ports.out.IModelRepository;

/**
 *
 * @author kotteletfisk
 */
public class PostgresModelRepository implements IModelRepository {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // private final SQLConnector sqlConnector;
    private final IPostgresConnectionSupplier connectionSupplier;

    public PostgresModelRepository(IPostgresConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO, List<OrderLineDTO> orderLineDTOS) throws SQLException {

        try (Connection conn = connectionSupplier.getConnection()) {

            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            PreparedStatement orderStmt = null;
            PreparedStatement lineStmt = null;

            try {
                // Insert order with provided int orderId
                String insertOrderSql = "INSERT INTO \"orders\" "
                        + "(order_id, customer_id, order_created, order_updated, order_status, supplier_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";

                orderStmt = conn.prepareStatement(insertOrderSql);
                orderStmt.setObject(1, orderDTO.getOrder_id());
                orderStmt.setObject(2, orderDTO.getCustomer_id(), Types.INTEGER);
                orderStmt.setTimestamp(3, orderDTO.getOrder_created());
                orderStmt.setTimestamp(4, orderDTO.getOrder_updated());
                orderStmt.setString(5, orderDTO.getOrderStatus().name());

                // TODO: nullable for compatiblity
                if (orderDTO.getSupplierId() != null) {
                    orderStmt.setInt(6, orderDTO.getSupplierId());
                } else {
                    orderStmt.setNull(6, 0);
                }

                int affectedOrders = orderStmt.executeUpdate();
                if (affectedOrders != 1) {
                    throw new SQLException("Failed to insert order, affected rows: " + affectedOrders);
                }

                // Insert order lines using same order_id
                String insertLineSql = "INSERT INTO order_line "
                        + "(order_id, item_id, price_snapshot, amount) "
                        + "VALUES (?, ?, ?, ?)";

                lineStmt = conn.prepareStatement(insertLineSql);

                for (OrderLineDTO lineDTO : orderLineDTOS) {
                    lineStmt.setObject(1, orderDTO.getOrder_id());
                    lineStmt.setInt(2, lineDTO.getItemId());
                    lineStmt.setFloat(3, lineDTO.getPriceSnapshot());
                    lineStmt.setInt(4, lineDTO.getAmount());
                    lineStmt.addBatch();
                }

                lineStmt.executeBatch();

                conn.commit();
                return orderDTO;

            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw ex;
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {
                }

                if (orderStmt != null)
                    try {
                        orderStmt.close();
                    } catch (SQLException ignored) {
                    }
                if (lineStmt != null)
                    try {
                        lineStmt.close();
                    } catch (SQLException ignored) {
                    }
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    public OrderDetailsDTO customerEnrichLegacyOrder(LegacyOrderDetailsDTO legacyDTO) throws SQLException {

        try (Connection connection = connectionSupplier.getConnection()) {
            log.info("Creating legacy order");

            // Query for existing customer
            String sql = "SELECT customer_id FROM customer WHERE customer_phone = ?";

            try (var queryStmnt = connection.prepareStatement(sql)) {

                queryStmnt.setString(1, legacyDTO.getCustomerPhone());

                ResultSet rs = queryStmnt.executeQuery();

                int customerId = 0;

                if (rs.next()) {
                    customerId = rs.getInt(1);
                    log.debug("Customer id: " + customerId + " matched with phone: " + legacyDTO.getCustomerPhone());
                } else {
                    log.info("No match customer match found. Creating anonymously");
                    createAnonUser(connection);
                }

                int index = 1;
                for (var line : legacyDTO.getOrderLineDTOS()) {
                    line.setOrderLineId(index++);
                }
                OrderDetailsDTO orderDetailsDTO = OrderDetailsDTO.builder()
                        .orderId(legacyDTO.getOrderId())
                        .customerId(customerId)
                        .status(OrderDetailsDTO.orderStatus.created)
                        .supplierId(legacyDTO.getSupplierId())
                        .orderLineDTOS(legacyDTO.getOrderLineDTOS())
                        .build();

                log.debug("Enriched dto:\n" + orderDetailsDTO.toString());

                return orderDetailsDTO;
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<menuItemDTO> getMenuItemsBySupplierId(int supplierId) throws SQLException {

        try (Connection connection = connectionSupplier.getConnection()) {
            String sql = "SELECT item_id, item_name, item_price, supplier_id, is_active "
                    + "FROM menu_item WHERE supplier_id = ?";

            try (var queryStmnt = connection.prepareStatement(sql)) {

                queryStmnt.setInt(1, supplierId);

                ResultSet rs = queryStmnt.executeQuery();

                List<menuItemDTO> items = new java.util.ArrayList<>();

                while (rs.next()) {
                    menuItemDTO item = new menuItemDTO(
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getDouble("item_price"),
                            rs.getInt("supplier_id"),
                            rs.getBoolean("is_active"));
                    items.add(item);
                }
                return items;
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }
    
    @Override
    public boolean healthCheck() throws Exception {

        for (int i = 0; i < 10; i++) {
            try {
                Connection connection = connectionSupplier.getConnection();

                if (connection.isValid(2)) {
                    return true;
                }

            } catch (PSQLException e) {
                log.warn(e.getLocalizedMessage());
                log.warn("Retrying PostgreSQL connection");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    log.error(ex.getLocalizedMessage());
                }
            }
        }
        throw new SQLException("Connection to PosgtreSQL failed!");
    }

    private void createAnonUser(Connection connection) throws SQLException {

        // Insert new anon user if not already exists
        String insertAnonSql = "INSERT INTO customer "
                + "(customer_id, customer_name, customer_zip, customer_phone) "
                + "VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING";

        try (var stmt = connection.prepareStatement(insertAnonSql)) {

            stmt.setInt(1, 0);
            stmt.setString(2, "Anonymous");
            stmt.setString(3, "0");
            stmt.setString(4, "0");

            if (stmt.executeUpdate() == 1) {
                log.info("Created new anonymous user");
            }

        } catch (SQLException e) {
            log.error("Failed to check/create anon user");
            throw new SQLException(e);
        }

    }
}
