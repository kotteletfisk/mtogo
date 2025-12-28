package mtogo.sql.adapter.out;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtogo.sql.adapter.persistence.IPostgresConnectionSupplier;
import mtogo.sql.model.DTO.AuthDTO;
import mtogo.sql.ports.out.IAuthRepository;

public class PostgresAuthRepository implements IAuthRepository {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // private final SQLConnector connector;
    private final IPostgresConnectionSupplier connectionSupplier;

    public PostgresAuthRepository(IPostgresConnectionSupplier connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public AuthDTO fetchAuthPerEmail(String email) {

        try {
            var conn = connectionSupplier.getConnection();

            var userStmt = conn.prepareStatement("""
                        SELECT user_id, email, password_hash
                        FROM auth_user
                        WHERE email = ?
                    """);
            userStmt.setString(1, email);
            var rs = userStmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            AuthDTO authDTO = new AuthDTO();

            authDTO.id = rs.getLong("user_id");
            authDTO.passwordHash = rs.getString("password_hash");
            authDTO.email = rs.getString("email");
            return authDTO;
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public List<String> fetchRolesForAuth(long id) {
        try {
            var conn = connectionSupplier.getConnection();

            var rolesStmt = conn.prepareStatement("""
                        SELECT role_name FROM auth_user_role WHERE user_id = ?
                    """);
            rolesStmt.setLong(1, id);
            var rolesRs = rolesStmt.executeQuery();

            List<String> roles = new ArrayList<>();
            while (rolesRs.next()) {
                roles.add(rolesRs.getString("role_name"));
            }
            return roles;
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public String fetchActorIdForAuth(String cred, String service) {
        try {
            var conn = connectionSupplier.getConnection();

            switch (service.toLowerCase()) {
                case "customer": {
                    var stmt = conn.prepareStatement("""
                                SELECT customer_id FROM customer WHERE customer_creds = ?
                            """);
                    stmt.setString(1, cred);
                    var rs = stmt.executeQuery();
                    if (rs.next()) {
                        int result = rs.getInt("customer_id");
                        return String.valueOf(result);
                    }
                }
                case "supplier": {
                    var stmt = conn.prepareStatement("""
                                SELECT supplier_id FROM supplier WHERE supplier_creds = ?
                            """);
                    stmt.setString(1, cred);
                    var rs = stmt.executeQuery();
                    if (rs.next()) {
                        int result = rs.getInt("supplier_id");
                        return String.valueOf(result);
                    }
                }
                case "courier": {
                    var stmt = conn.prepareStatement("""
                                SELECT courier_id FROM courier WHERE courier_creds = ?
                            """);
                    stmt.setString(1, cred);
                    var rs = stmt.executeQuery();
                    if (rs.next()) {
                        int result = rs.getInt("courier_id");
                        return String.valueOf(result);
                    }
                }
                case "management": {
                    //TODO: Implement later
                }
                case "support": {
                    //TODO: Implement later
                }
                default: {
                    return String.valueOf(-2);
                }
            }
        } catch (SQLException e) {
            return String.valueOf(-1);
        }
    }

    @Override
    public boolean healthCheck() throws Exception {

        for (int i = 0; i < 10; i++) {
            try {
                Connection conn = connectionSupplier.getConnection();
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
