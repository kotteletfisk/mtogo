package mtogo.sql.adapter.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SQLConnector {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

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
        log.debug("Env found for key {}: {}", key, val);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }
}