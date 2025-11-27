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
import mtogo.sql.DTO.menuItemDTO;

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
        String host = "mtogo-db";
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

    public List<menuItemDTO> getMenuItemsBySupplierId(int supplierId, Connection connection) throws SQLException {

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
                        rs.getBoolean("is_active")
                );
                items.add(item);
            }
            return items;
        }
    }

    public OrderDTO createLegacyOrder(LegacyOrderDetailsDTO legacyDTO,
            Connection connection) throws SQLException {

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
            OrderDTO orderDTO = new OrderDTO(legacyDTO.getOrderId(), customerId);

            int index = 1;
            for (var line : legacyDTO.getOrderLineDTOS()) {
                line.setOrderLineId(index++);
            }

            return createOrder(orderDTO, legacyDTO.getOrderLineDTOS(), connection);
        }
    }

    private void createAnonUser(Connection connection) throws SQLException {

        // Insert new anon user if not already exists
        String insertAnonSql
                = "INSERT INTO customer "
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
