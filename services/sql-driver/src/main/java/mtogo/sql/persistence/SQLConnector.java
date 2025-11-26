package mtogo.sql.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderLineDTO;

public class SQLConnector {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Opens a new connection to the Postgres database.
     *
     * Uses env vars so it works in Docker and locally: - MTOGO_DB_HOST (e.g.
     * MToGo-db in docker, localhost locally) - MTOGO_DB_PORT (default 5432) -
     * MTOGO_DB (db name) - MTOGO_USER (username) - MTOGO_PASS (password)
     */
    //Used for local testing
    /*public Connection getConnection() throws SQLException {
        String host = "localhost";
        String port = "5432";
        String db   = "mtogo";
        String user = "mtogo";
        String pass = "mtogo";

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        return DriverManager.getConnection(url, user, pass);
    }

     */
    public Connection getConnection() throws SQLException {
        String host = "MToGo-db";
        String port = "5432";
        String db = envOrDefault("POSTGRES_DB", "mtogo");
        String user = envOrDefault("POSTGRES_USER", "mtogo");
        String pass = envOrDefault("POSTGRES_PASSWORD", "mtogo");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        return DriverManager.getConnection(url, user, pass);
    }

    private String envOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }

    /**
     * Creates an order and its order lines in a single transaction.
     *
     * The orderId is assumed to be generated in customer-service and passed in
     * via orderDTO.getOrder_id().
     *
     * If anything fails, the transaction is rolled back so that neither the
     * order or any order_line rows are persisted.
     */
    public OrderDTO createOrder(OrderDTO orderDTO,
            List<OrderLineDTO> orderLineDTOS,
            Connection connection) throws SQLException {

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        PreparedStatement orderStmt = null;
        PreparedStatement lineStmt = null;

        try {
            // Insert order with provided int orderId
            String insertOrderSql
                    = "INSERT INTO \"orders\" "
                    + "(order_id, customer_id, order_created, order_updated, order_status) "
                    + "VALUES (?, ?, ?, ?, ?)";

            orderStmt = connection.prepareStatement(insertOrderSql);
            orderStmt.setObject(1, orderDTO.getOrder_id());
            orderStmt.setObject(2, orderDTO.getCustomer_id(), Types.INTEGER);
            orderStmt.setTimestamp(3, orderDTO.getOrder_created());
            orderStmt.setTimestamp(4, orderDTO.getOrder_updated());
            orderStmt.setString(5, orderDTO.getOrderStatus().name());

            int affectedOrders = orderStmt.executeUpdate();
            if (affectedOrders != 1) {
                throw new SQLException("Failed to insert order, affected rows: " + affectedOrders);
            }

            // Insert order lines using same order_id
            String insertLineSql
                    = "INSERT INTO order_line "
                    + "(order_id, item_id, price_snapshot, amount) "
                    + "VALUES (?, ?, ?, ?)";

            lineStmt = connection.prepareStatement(insertLineSql);

            for (OrderLineDTO lineDTO : orderLineDTOS) {
                lineStmt.setObject(1, orderDTO.getOrder_id());
                lineStmt.setInt(2, lineDTO.getItemId());
                lineStmt.setFloat(3, lineDTO.getPriceSnapshot());
                lineStmt.setInt(4, lineDTO.getAmount());
                lineStmt.addBatch();
            }

            lineStmt.executeBatch();

            connection.commit();
            return orderDTO;

        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {
            }

            if (orderStmt != null) try {
                orderStmt.close();
            } catch (SQLException ignored) {
            }
            if (lineStmt != null) try {
                lineStmt.close();
            } catch (SQLException ignored) {
            }
        }

    }

    public OrderDTO createLegacyOrder(LegacyOrderDetailsDTO legacyDTO,
            Connection connection) throws SQLException {
            
            log.info("Creating legacy order");

        // Query for existing customer
        // FIXME: relation 'customer' not found
        String sql = "SELECT customer_id FROM customer WHERE customer_phone = ?";

        try (var queryStmnt = connection.prepareStatement(sql)) {

            queryStmnt.setString(1, legacyDTO.getCustomerPhone());

            ResultSet rs = queryStmnt.executeQuery();

            Integer customerId = null;

            if (rs.next()) {
                customerId = rs.getInt(1);
                log.debug("Customer id: " + customerId + " matched with phone: " + legacyDTO.getCustomerPhone());
            }
            else {
                log.info("No match customer match found. Creating anonymously");
            }
            OrderDTO orderDTO = new OrderDTO(legacyDTO.getOrderId(), customerId);

            int index = 1;
            for (var line : legacyDTO.getOrderLineDTOS()) {
                line.setOrderLineId(index++);
            }

            return createOrder(orderDTO, legacyDTO.getOrderLineDTOS(), connection);
        }
    }
}
