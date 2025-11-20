import mtogo.sql.DTO.OrderDTO;
import mtogo.sql.DTO.OrderLineDTO;
import mtogo.sql.persistence.SQLConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQLConnectorTest {

    private Connection conn;
    private SQLConnector connector;

    @Mock
    OrderDTO order;

    @Mock
    OrderLineDTO line1;

    @Mock
    OrderLineDTO line2;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;MODE=PostgreSQL",
                "sa",
                ""
        );

        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE customer (
                    customer_id INT PRIMARY KEY,
                    customer_name VARCHAR(50)
                );
            """);

            st.execute("""
                CREATE DOMAIN orderstatus AS VARCHAR(50);
            """);

            st.execute("""
                CREATE TABLE "orders" (
                    order_id UUID PRIMARY KEY,
                    customer_id INT NOT NULL,
                    order_created TIMESTAMP NOT NULL,
                    order_updated TIMESTAMP NOT NULL,
                    order_status orderstatus NOT NULL,
                    CONSTRAINT fk_customer_id FOREIGN KEY (customer_id)
                        REFERENCES customer(customer_id)
                );
            """);

            st.execute("""
                CREATE TABLE menu_item (
                    item_id INT PRIMARY KEY
                );
            """);

            st.execute("""
                CREATE TABLE order_line (
                    order_line_id SERIAL PRIMARY KEY,
                    order_id UUID NOT NULL,
                    item_id INT NOT NULL,
                    price_snapshot REAL NOT NULL,
                    amount INT NOT NULL,
                    CONSTRAINT fk_order_id FOREIGN KEY (order_id)
                        REFERENCES "orders"(order_id),
                    CONSTRAINT fk_item_id FOREIGN KEY (item_id)
                        REFERENCES menu_item(item_id)
                );
            """);
        }

        connector = new SQLConnector();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    void createOrder_insertsOrderAndLines() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO customer (customer_id, customer_name) VALUES (1, 'Test Customer');");
            st.execute("INSERT INTO menu_item (item_id) VALUES (10);");
            st.execute("INSERT INTO menu_item (item_id) VALUES (11);");
        }

        UUID orderId = UUID.randomUUID();
        Timestamp created = new Timestamp(System.currentTimeMillis());
        Timestamp updated = new Timestamp(System.currentTimeMillis());

        when(order.getOrder_id()).thenReturn(orderId);
        when(order.getCustomer_id()).thenReturn(1);
        when(order.getOrder_created()).thenReturn(created);
        when(order.getOrder_updated()).thenReturn(updated);
        when(order.getOrderStatus()).thenReturn(OrderDTO.orderStatus.created);

        when(line1.getItem_id()).thenReturn(10);
        when(line1.getPrice_snapshot()).thenReturn(50.0f);
        when(line1.getAmount()).thenReturn(2);

        when(line2.getItem_id()).thenReturn(11);
        when(line2.getPrice_snapshot()).thenReturn(70.0f);
        when(line2.getAmount()).thenReturn(1);

        connector.createOrder(order, List.of(line1, line2), conn);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM \"orders\" WHERE order_id = ?")) {
            ps.setObject(1, orderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM order_line WHERE order_id = ?")) {
            ps.setObject(1, orderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void createOrder_rollsBackWhenOrderLineInsertFails() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO customer (customer_id, customer_name) VALUES (1, 'Test Customer');");
        }

        UUID orderId = UUID.randomUUID();
        Timestamp created = new Timestamp(System.currentTimeMillis());
        Timestamp updated = new Timestamp(System.currentTimeMillis());

        when(order.getOrder_id()).thenReturn(orderId);
        when(order.getCustomer_id()).thenReturn(1);
        when(order.getOrder_created()).thenReturn(created);
        when(order.getOrder_updated()).thenReturn(updated);
        when(order.getOrderStatus()).thenReturn(OrderDTO.orderStatus.created);

        OrderLineDTO badLine = mock(OrderLineDTO.class);
        when(badLine.getItem_id()).thenReturn(999);
        when(badLine.getPrice_snapshot()).thenReturn(10.0f);
        when(badLine.getAmount()).thenReturn(1);

        assertThrows(SQLException.class, () ->
                connector.createOrder(order, List.of(badLine), conn)
        );

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM \"orders\" WHERE order_id = ?")) {
            ps.setObject(1, orderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM order_line WHERE order_id = ?")) {
            ps.setObject(1, orderId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
}
