/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import mtogo.sql.adapter.out.PostgresAuthRepository;
import mtogo.sql.model.DTO.AuthDTO;
import mtogo.sql.ports.out.IAuthRepository;

/**
 *
 * @author kotteletfisk
 */

@ExtendWith(MockitoExtension.class)
public class AuthRepoTest {

    private Connection conn;
    private IAuthRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection(
                "jdbc:h2:mem:testdbauth;MODE=PostgreSQL",
                "sa",
                "");
        try (Statement st = conn.createStatement()) {
            st.execute("""
                        DROP TABLE IF EXISTS public.auth_user;
                        CREATE TABLE public.auth_user (
                            user_id SERIAL PRIMARY KEY,
                            email VARCHAR(255) NOT NULL UNIQUE,
                            password_hash VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                            );

                    """);

            st.execute("""
                        DROP TABLE IF EXISTS public.auth_user_role;
                        CREATE TABLE public.auth_user_role (
                            user_id INT NOT NULL,
                            role_name VARCHAR(50) NOT NULL,
                            PRIMARY KEY (user_id, role_name)
                            );
                    """);

            st.execute("""
                    DROP TABLE IF EXISTS public.supplier;
                    CREATE TABLE IF NOT EXISTS public.supplier
                    (
                        supplier_id serial NOT NULL,
                        supplier_name character varying(50) NOT NULL,
                        supplier_zip character varying(10) NOT NULL,
                        supplier_creds character varying(50),
                        PRIMARY KEY (supplier_id)
                    );
                                        """);

            st.execute("""
                    DROP TABLE IF EXISTS public.customer;
                    CREATE TABLE public.customer
                    (
                        customer_id serial NOT NULL,
                        customer_name character varying(50) NOT NULL,
                        customer_zip character varying(10) NOT NULL,
                        customer_phone character varying(15) NOT NULL,
                        customer_creds character varying(50),
                        PRIMARY KEY (customer_id)
                    );
                    """);
            st.execute("""
                    DROP TABLE IF EXISTS public.courier;
                    CREATE TABLE IF NOT EXISTS public.courier
                    (
                        courier_id serial NOT NULL,
                        courier_name character varying(50) NOT NULL,
                        courier_creds character varying(50),
                        PRIMARY KEY (courier_id)
                    );
                    """);
        }

        repository = new PostgresAuthRepository(() -> conn);
    }

    @Test
    void fetchAuthDataOnEmailMatch() throws SQLException {

        // Fetch correct auth information from repo on valid email
        String email = "test@customer.com";

        try (Statement st = conn.createStatement()) {
            st.execute(String.format("""
                        INSERT INTO public.auth_user (user_id, email, password_hash)
                        VALUES (1, '%s', 'fffff');
                    """, email));
            st.execute("""
                        INSERT INTO public.auth_user_role (user_id, role_name)
                        VALUES (1, 'customer');
                    """);
        }

        AuthDTO result = repository.fetchAuthPerEmail(email);

        assertEquals(email, result.email);
        assertEquals("fffff", result.passwordHash);
    }

    @Test
    void returnNullOnNoMatch() throws SQLException {

        String email = "test@customer.com";

        try (Statement st = conn.createStatement()) {
            st.execute(String.format("""
                        INSERT INTO public.auth_user (user_id, email, password_hash)
                        VALUES (1, '%s', 'fffff');
                    """, email));
            st.execute("""
                        INSERT INTO public.auth_user_role (user_id, role_name)
                        VALUES (1, 'customer');
                    """);
        }

        AuthDTO result = repository.fetchAuthPerEmail("nonexistent@customer.com");

        assertNull(result);
    }

    @Test
    void fetchRolesOnValidAuth() throws SQLException {

        String email = "test@customer.com";
        long id = 1;

        try (Statement st = conn.createStatement()) {
            st.execute(String.format("""
                        INSERT INTO public.auth_user (user_id, email, password_hash)
                        VALUES (%d, '%s', 'fffff');
                    """, id, email));
            st.execute("""
                        INSERT INTO public.auth_user_role (user_id, role_name)
                        VALUES (1, 'customer');
                    """);
        }

        List<String> roles = repository.fetchRolesForAuth(id);

        assertEquals(roles.size(), 1);
        assertEquals(roles.get(0), "customer");
    }

    @Test
    void returnEmptyListOnNoRoleMatch() throws SQLException {

        String email = "test@customer.com";
        long id = 1;

        try (Statement st = conn.createStatement()) {
            st.execute(String.format("""
                        INSERT INTO public.auth_user (user_id, email, password_hash)
                        VALUES (%d, '%s', 'fffff');
                    """, id, email));
            st.execute("""
                        INSERT INTO public.auth_user_role (user_id, role_name)
                        VALUES (1, 'customer');
                    """);
        }

        List<String> roles = repository.fetchRolesForAuth(2); // Another id

        assertNotNull(roles);
        assertEquals(roles.size(), 0);
    }

    @ParameterizedTest
    @CsvSource({
            "pizza@example.com,supplier,1",
            "sushi@example.com,supplier,3",
            "alice@example.com,customer,1",
            "carl@example.com,courier,1"
    })
    void fetchActorIDfromCredentials(String cred, String service, String expected) throws SQLException {

        try (Statement st = conn.createStatement()) {
            st.execute("""
                    TRUNCATE TABLE public.supplier;
                    INSERT INTO public.supplier (supplier_id, supplier_name, supplier_zip, supplier_creds)
                    VALUES
                        (1, 'Pizza Palace',      '2100', 'pizza@example.com'),
                        (2, 'Burger Barn',       '2200', 'burger@example.com'),
                        (3, 'Sushi Spot',        '2300', 'sushi@example.com');

                                        """);
            st.execute(
                    """
                            TRUNCATE TABLE public.customer;
                            INSERT INTO public.customer (customer_id, customer_name, customer_zip, customer_phone, customer_creds)
                            VALUES
                                (1, 'Alice Andersen',    '2100', '+45 11111111', 'alice@example.com'),
                                (2, 'Bob Bæk',           '2200', '+45 22222222', 'bob@example.com'),
                                (3, 'Charlotte Christensen', '2300', '+45 33333333', 'charlotte@example.com');

                                                """);
            st.execute(
                    """
                            TRUNCATE TABLE public.courier;
                            INSERT INTO public.courier (courier_id, courier_name, courier_creds)
                            VALUES
                                (1, 'Carl Courier', 'carl@example.com'),
                                (2, 'Dorthe Delivery', 'dorthe@example.com');
                                                                    """);
        }

        String id = repository.fetchActorIdForAuth(cred, service);

        assertEquals(id, expected);
    }

    @Test
    void returnNegativeTwoOnNoCredMatch() {

        String cred = "nonexistent";
        String service = "supplier";

        String id = repository.fetchActorIdForAuth(cred, service);

        assertEquals("-2", id);
    }

    @Test
    void returnNegativeTwoOnNoSerivceMatch() throws SQLException {

        String cred = "pizza@example.com";
        String service = "nonexistent";

        try (Statement st = conn.createStatement()) {
            st.execute("""
                    TRUNCATE TABLE public.supplier;
                    INSERT INTO public.supplier (supplier_id, supplier_name, supplier_zip, supplier_creds)
                    VALUES
                        (1, 'Pizza Palace',      '2100', 'pizza@example.com'),
                        (2, 'Burger Barn',       '2200', 'burger@example.com'),
                        (3, 'Sushi Spot',        '2300', 'sushi@example.com');

                                        """);
        }

        String id = repository.fetchActorIdForAuth(cred, service);

        assertEquals("-2", id);
    }

    @Test
    void healthCheckValidTest() throws Exception {

        assertTrue(repository.healthCheck());
    }

    @Test
    void throwOnHealthCheckNonValidConnection() throws SQLException {

        conn.close(); // close connection for introducing non-valid state

        assertThrows(SQLException.class, repository::healthCheck);
    }
}
