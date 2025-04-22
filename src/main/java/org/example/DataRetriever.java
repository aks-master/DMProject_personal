package org.example;

import java.sql.*;

public class DataRetriever {
    public static void displayInsertedTables() {
        String dbName = DatabaseConnector.getDatabaseName();
        try (Connection conn = DatabaseConnector.getConnection()) {
            System.out.println("\nDisplaying tables for database: " + dbName);

            // Get database metadata
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(dbName, null, "%", new String[]{"TABLE"});

            boolean hasAnyTable = false;

            // Display each table
            while (tables.next()) {
                hasAnyTable = true;
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("\nTable: " + tableName);
                displayTableData(conn, tableName);
            }

            if (!hasAnyTable) {
                System.out.println("No tables found in the database.");
            }

            // this code shows relatipships table again hence commented
            // Try to display relationships table if it exists
//            try {
//                System.out.println("\nAttempting to show relationship tables:");
//                ResultSet relationshipTables = metaData.getTables(dbName, null, "%relationships", new String[]{"TABLE"});
//                while (relationshipTables.next()) {
//                    String tableName = relationshipTables.getString("TABLE_NAME");
//                    System.out.println("\nRelationship table: " + tableName);
//                    displayTableData(conn, tableName);
//                }
//            } catch (SQLException e) {
//                // Silently ignore if no relationship tables exist
//            }

        } catch (SQLException e) {
            System.out.println("Error retrieving database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void displayTableData(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 10")) {

            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnCount = rsMetaData.getColumnCount();

            // Print column headers
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rsMetaData.getColumnName(i) + "\t");
            }
            System.out.println();

            // Print data rows
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }

            if (!hasData) {
                System.out.println("(No data)");
            }
        } catch (SQLException e) {
            System.out.println("Error displaying table " + tableName + ": " + e.getMessage());
        }
    }
}