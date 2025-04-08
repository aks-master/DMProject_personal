package org.example;

import java.sql.*;

public class DataRetriever {
    public static void displayInsertedTables() {
        String[] tables = {"store", "category", "subcategory", "product"};

        try (Connection conn = DatabaseConnector.getConnection()) {
            for (String table : tables) {
                String query = "SELECT * FROM " + table;
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    System.out.println("\nTable: " + table);
                    while (rs.next()) {
                        for (int i = 1; i <= colCount; i++) {
                            System.out.print(meta.getColumnName(i) + ": " + rs.getString(i) + "  ");
                        }
                        System.out.println();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
