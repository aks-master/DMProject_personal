package org.example;

import java.sql.*;
import java.util.List;

public class DatabaseConnector {
    private static final String URL = "jdbc:mysql://localhost:3306/xsddb";
    private static final String USER = "xsduser";
    private static final String PASSWORD = "1234";

    // Method to execute table creation SQL dynamically
    public static void executeTableCreationSQL(List<String> tableDefinitions) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            for (String sql : tableDefinitions) {
                stmt.execute(sql);
                System.out.println("Executed: " + sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to insert data into dynamically created tables
    public static int executeInsertAndGetGeneratedKey(String sql, String[] generatedColumns, Object[] parameters) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql, generatedColumns)) {
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);  // Return the generated key
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;  // If no key is generated
    }
}
