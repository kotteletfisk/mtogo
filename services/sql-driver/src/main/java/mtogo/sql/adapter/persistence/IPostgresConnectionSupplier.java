/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package mtogo.sql.adapter.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author kotteletfisk
 */
@FunctionalInterface
public interface IPostgresConnectionSupplier {

    public Connection getConnection() throws SQLException;
}
