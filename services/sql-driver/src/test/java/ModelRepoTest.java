
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import mtogo.sql.adapter.out.PostgresModelRepository;
import mtogo.sql.adapter.persistence.SQLConnector;
import mtogo.sql.model.DTO.LegacyOrderDetailsDTO;
import mtogo.sql.model.DTO.OrderDTO;
import mtogo.sql.model.DTO.OrderDetailsDTO;
import mtogo.sql.model.DTO.OrderLineDTO;
import mtogo.sql.ports.out.IModelRepository;

@ExtendWith(MockitoExtension.class)
class ModelRepoTest {

    private Connection conn;
    private Connection repoConn;
    private IModelRepository repository;

    @Mock
    SQLConnector connector;

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
                "");  

        repoConn = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;MODE=PostgreSQL",
                "sa",
                "");

        try (Statement st = conn.createStatement()) {
            st.execute("""
                        CREATE TABLE customer (
                            customer_id serial NOT NULL,
                            customer_name character varying(50) NOT NULL,
                            customer_zip character varying(10) NOT NULL,
                            customer_phone character varying(15) NOT NULL,
                            customer_creds character varying(50),
                            PRIMARY KEY (customer_id)
                        );

                    """);

            st.execute("""
                        CREATE DOMAIN orderstatus AS VARCHAR(50);
                    """);

            st.execute("""
                        CREATE TABLE "orders" (
                            order_id UUID PRIMARY KEY,
                            customer_id INT,
                            supplier_id INT,
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
                        st.execute("""
                            CREATE TABLE supplier (
                        supplier_id serial NOT NULL,
                        supplier_name character varying(50) NOT NULL,
                        supplier_zip character varying(10) NOT NULL,
                        supplier_creds character varying(50),
                        PRIMARY KEY (supplier_id)
                        );

                                    """);
        }

        // inject test connection
        when(connector.getConnection()).thenReturn(repoConn);

        repository = new PostgresModelRepository(connector);
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
            st.execute(
                    "INSERT INTO customer (customer_name, customer_zip, customer_phone) VALUES ('Test Customer', '2450', '22222222');");
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

        when(line1.getItemId()).thenReturn(10);
        when(line1.getPriceSnapshot()).thenReturn(50.0f);
        when(line1.getAmount()).thenReturn(2);

        when(line2.getItemId()).thenReturn(11);
        when(line2.getPriceSnapshot()).thenReturn(70.0f);
        when(line2.getAmount()).thenReturn(1);

        repository.createOrder(order, List.of(line1, line2));

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
            st.execute(
                    "INSERT INTO customer (customer_name, customer_zip, customer_phone) VALUES ('Test Customer', '2450', '22222222');");
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
        when(badLine.getItemId()).thenReturn(999);
        when(badLine.getPriceSnapshot()).thenReturn(10.0f);
        when(badLine.getAmount()).thenReturn(1);

        assertThrows(Exception.class, () -> repository.createOrder(order, List.of(badLine)));

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

    @Test
    void enrichLegacyOrderWithMatchedCustomer() throws Exception {

        try (Statement st = conn.createStatement()) {
            st.execute(
                    "INSERT INTO customer (customer_id, customer_name, customer_zip, customer_phone) VALUES (1, 'Test Customer', '2450', '11111111');");
            st.execute(
                    "INSERT INTO supplier (supplier_id, supplier_name, supplier_zip) VALUES (1, 'Test Supplier', '2450');");
            st.execute("INSERT INTO menu_item (item_id) VALUES (10);");
            st.execute("INSERT INTO menu_item (item_id) VALUES (11);");
        }

        UUID id = UUID.randomUUID();
        LegacyOrderDetailsDTO legacy = new LegacyOrderDetailsDTO(
                id,
                "11111111", // Phone number exists
                1,
                List.of(
                        new OrderLineDTO(id, 10, 1.0f, 1),
                        new OrderLineDTO(id, 11, 2.0f, 2)));

        OrderDetailsDTO dto = repository.customerEnrichLegacyOrder(legacy);

        // Legacy order matched with existing customer
        assertEquals(1, dto.getCustomerId());
    }

    @Test
    void enrichLegacyOrderWithUnmatchedAnonymousCustomer() throws Exception {

        try (Statement st = conn.createStatement()) {
            st.execute(
                    "INSERT INTO customer (customer_id, customer_name, customer_zip, customer_phone) VALUES (1, 'Test Customer', '2450', '11111111');");
            st.execute(
                    "INSERT INTO supplier (supplier_id, supplier_name, supplier_zip) VALUES (1, 'Test Supplier', '2450');");
            st.execute("INSERT INTO menu_item (item_id) VALUES (10);");
            st.execute("INSERT INTO menu_item (item_id) VALUES (11);");
        }

        UUID id = UUID.randomUUID();
        LegacyOrderDetailsDTO legacy = new LegacyOrderDetailsDTO(
                id,
                "22222222", // Phone number exists
                1,
                List.of(
                        new OrderLineDTO(id, 10, 1.0f, 1),
                        new OrderLineDTO(id, 11, 2.0f, 2)));

        OrderDetailsDTO dto = repository.customerEnrichLegacyOrder(legacy);

        // Legacy order set to anonymous customer id 0 on no match
        assertEquals(0, dto.getCustomerId());
    }
    /*
     * @Test
     * void createAnonymousCustomerOnNoMatchedLegacyOrder() throws SQLException {
     * 
     * try (Statement st = conn.createStatement()) {
     * st.
     * execute("INSERT INTO customer (customer_name, customer_zip, customer_phone) VALUES ('Test Customer', '2450', '22222222');"
     * );
     * st.execute("INSERT INTO menu_item (item_id) VALUES (10);");
     * st.execute("INSERT INTO menu_item (item_id) VALUES (11);");
     * }
     * 
     * UUID id = UUID.randomUUID();
     * LegacyOrderDetailsDTO legacy = new LegacyOrderDetailsDTO(
     * id,
     * "11111111",
     * List.of(
     * new OrderLineDTO(id, 10, 1.0f, 1),
     * new OrderLineDTO(id, 11, 2.0f, 2)
     * )
     * );
     * 
     * try (PreparedStatement ps =
     * conn.prepareStatement("SELECT * FROM customer WHERE customer_id = ?")) {
     * 
     * ps.setInt(1, 0);
     * ResultSet rs = ps.executeQuery();
     * 
     * // No anonymous user exists with id 0
     * assertFalse(rs.next());
     * }
     * 
     * // Create legacy order with uknown customer
     * connector.createLegacyOrder(legacy, conn);
     * 
     * try (PreparedStatement ps =
     * conn.prepareStatement("SELECT * FROM customer WHERE customer_id = ?")) {
     * 
     * ps.setInt(1, 0);
     * ResultSet rs = ps.executeQuery();
     * 
     * // Anonymous user exists after non-matched legacy order created
     * assertTrue(rs.next());
     * 
     * assertEquals(0, rs.getInt("customer_id"));
     * assertEquals("Anonymous", rs.getString("customer_name"));
     * }
     * 
     * try (PreparedStatement ps =
     * conn.prepareStatement("SELECT * FROM \"orders\" WHERE customer_id = ?")) {
     * 
     * ps.setInt(1, 0);
     * ResultSet rs = ps.executeQuery();
     * 
     * // Order created with anonymous customer
     * assertTrue(rs.next());
     * }
     * }
     * 
     * @Test
     * void matchLegacyOrderWithExistingAnonymousCustomer() throws SQLException {
     * 
     * try (Statement st = conn.createStatement()) {
     * st.
     * execute("INSERT INTO customer (customer_id, customer_name, customer_zip, customer_phone) VALUES (0, 'Anonymous', '0', '0');"
     * );
     * st.execute("INSERT INTO menu_item (item_id) VALUES (10);");
     * st.execute("INSERT INTO menu_item (item_id) VALUES (11);");
     * }
     * 
     * UUID id = UUID.randomUUID();
     * LegacyOrderDetailsDTO legacy = new LegacyOrderDetailsDTO(
     * id,
     * "11111111",
     * List.of(
     * new OrderLineDTO(id, 10, 1.0f, 1),
     * new OrderLineDTO(id, 11, 2.0f, 2)
     * )
     * );
     * 
     * try (PreparedStatement ps =
     * conn.prepareStatement("SELECT * FROM customer WHERE customer_id = ?")) {
     * 
     * ps.setInt(1, 0);
     * ResultSet rs = ps.executeQuery();
     * 
     * // Anonymous user exists with id 0
     * assertTrue(rs.next());
     * }
     * 
     * connector.createLegacyOrder(legacy, conn);
     * 
     * try (PreparedStatement ps =
     * conn.prepareStatement("SELECT * FROM \"orders\" WHERE customer_id = ?")) {
     * 
     * ps.setInt(1, 0);
     * ResultSet rs = ps.executeQuery();
     * 
     * // Order created with anonymous order
     * assertTrue(rs.next());
     * }
     * }
     */
}
