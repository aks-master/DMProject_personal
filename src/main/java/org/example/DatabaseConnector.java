package org.example;

import java.sql.*;
import java.util.List;

public class DatabaseConnector {
    private static final String URL = "jdbc:mysql://localhost:3306/xsddb";
    private static final String USER = "xsduser";
    private static final String PASSWORD = "1234";

    public static void executeSQL(List<String> sqlStatements) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            for (String sql : sqlStatements) {
                stmt.execute(sql);
                System.out.println("Executed: " + sql);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
