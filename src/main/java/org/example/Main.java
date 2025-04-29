package org.example;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Choose your desired option:");
            System.out.println("1. Process existing XML file and create database from it");
            System.out.println("2. Convert JSON file to the required XML format");
            System.out.print("Enter choice (1 or 2): ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    runXMLWorkflow();
                    break;
                case 2:
                    runJSONWorkflow();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }

            scanner.close();
        } finally {
            // Close connection pool when application exits
            DatabaseConnector.closePool();
        }
    }

    private static void runXMLWorkflow() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the path to the meta-meta-model XSD file: ");
            String metaMetaModelPath = scanner.nextLine();

            System.out.print("Enter the path to the meta-model XML file: ");
            String metaModelPath = scanner.nextLine();

            System.out.print("Enter the path to the data XML file: ");
            String dataXMLPath = scanner.nextLine();

            if (!XMLValidator.validateXMLSchema(metaMetaModelPath, metaModelPath)) {
                System.out.println("Validation failed: meta-model XML is not valid against meta-meta-model XSD.");
                return;
            }

            System.out.println("Validation successful.");

            // Extract database name from meta model file
            String dbName = extractDatabaseName(metaModelPath);

            // Create the database
            if (!DatabaseConnector.createDatabase(dbName)) {
                System.out.println("Failed to create database. Exiting.");
                return;
            }

            // Set the database name for all subsequent operations
            DatabaseConnector.setDatabaseName(dbName);

            List<String> sqlStatements = XSDToSQLConverter.parseXSD(metaModelPath);
            if (sqlStatements.isEmpty()) {
                System.out.println("No SQL statements were generated.");
                return;
            }
            System.out.println("SQL statements generated:");
            System.out.println(sqlStatements);

            // Step 1: Execute SQL statements to create tables
            DatabaseConnector.executeSQL(sqlStatements);

            // Step 2: Insert XML data into the database
            XMLDataInserter.insertStoreData(dataXMLPath);

            // Step 3: Retrieve and display star schema
            DataRetriever.displayInsertedTables();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runJSONWorkflow() {
        try {
            JSONToMetaModelAndXMLConverter.convert();

            System.out.println("Conversion complete. Check the output directory for meta-model and XML.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String extractDatabaseName(String metaModelPath) {
        // Extract the filename without extension
        File file = new File(metaModelPath);
        String fileName = file.getName();

        // Check if the filename follows the meta-model-<dbname>.xml pattern
        if (fileName.startsWith("meta-model-") && fileName.contains(".")) {
            return fileName.substring("meta-model-".length(), fileName.lastIndexOf('.'));
        }

        // If the filename doesn't match the pattern, use a default name with timestamp
        return "xsddb_" + System.currentTimeMillis();
    }
}
