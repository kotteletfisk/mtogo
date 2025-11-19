package mtogo.sql.persistence;

import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderLineDTO;

import java.sql.*;
import java.util.List;

public class SQLConnector {

    /**
     * Opens a new connection to the Postgres database.
     *
     * Uses env vars so it works in Docker and locally:
     *  - MTOGO_DB_HOST (e.g. MToGo-db in docker, localhost locally)
     *  - MTOGO_DB_PORT (default 5432)
     *  - MTOGO_DB     (db name)
     *  - MTOGO_USER   (username)
     *  - MTOGO_PASS   (password)
     */
    public Connection getConnection() throws SQLException {
        String host = envOrDefault("MTOGO_DB_HOST", "MToGo-db");
        String port = envOrDefault("MTOGO_DB_PORT", "5432");
        String db   = envOrDefault("MTOGO_DB", "mtogo");
        String user = envOrDefault("MTOGO_USER", "mtogo");
        String pass = envOrDefault("MTOGO_PASS", "mtogo");

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
     * The orderId is assumed to be generated in customer-service and passed
     * in via orderDTO.getOrder_id().
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
        PreparedStatement lineStmt  = null;

        try {
            // Insert order with provided int orderId
            String insertOrderSql =
                    "INSERT INTO \"orderDTO\" " +
                            "(order_id, customer_id, order_created, order_updated, order_status) " +
                            "VALUES (?, ?, ?, ?, ?)";

            orderStmt = connection.prepareStatement(insertOrderSql);
            orderStmt.setInt(1, orderDTO.getOrder_id());
            orderStmt.setInt(2, orderDTO.getCustomer_id());
            orderStmt.setTimestamp(3, orderDTO.getOrder_created());
            orderStmt.setTimestamp(4, orderDTO.getOrder_updated());
            orderStmt.setString(5, orderDTO.getOrderStatus().name());

            int affectedOrders = orderStmt.executeUpdate();
            if (affectedOrders != 1) {
                throw new SQLException("Failed to insert order, affected rows: " + affectedOrders);
            }

            // Insert order lines using same order_id
            String insertLineSql =
                    "INSERT INTO order_line " +
                            "(order_id, item_id, price_snapshot, amount) " +
                            "VALUES (?, ?, ?, ?)";

            lineStmt = connection.prepareStatement(insertLineSql);

            for (OrderLineDTO lineDTO : orderLineDTOS) {
                lineStmt.setInt(1, orderDTO.getOrder_id());
                lineStmt.setInt(2, lineDTO.getItem_id());
                lineStmt.setFloat(3, lineDTO.getPrice_snapshot());
                lineStmt.setInt(4, lineDTO.getAmount());
                lineStmt.addBatch();
            }

            lineStmt.executeBatch();

            // If all is good, then we commit
            connection.commit();
            return orderDTO;

        } catch (SQLException ex) {
            // Roll back if anything fails
            try {
                connection.rollback();
            } catch (SQLException ignored) {}
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {}

            if (orderStmt != null) try { orderStmt.close(); } catch (SQLException ignored) {}
            if (lineStmt != null) try { lineStmt.close(); } catch (SQLException ignored) {}
        }
    }
}
