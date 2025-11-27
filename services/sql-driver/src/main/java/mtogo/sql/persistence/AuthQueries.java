package mtogo.sql.persistence;

import mtogo.sql.DTO.AuthDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class AuthQueries {

    private final Connection conn;

    public AuthQueries(Connection conn) {
        this.conn = conn;
    }

    public AuthDTO fetchAuthPerEmail(String email) {
        try {
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
        }  catch (SQLException e) {
            return null;
        }
    }

    public List<String> fetchRolesForAuth(long id) {
        try {
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
        } catch (SQLException e){
            return null;
        }
    }
}
