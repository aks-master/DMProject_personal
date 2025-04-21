package org.example;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose input type:");
        System.out.println("1. XML");
        System.out.println("2. JSON");
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
    }

    private static void runXMLWorkflow() {
        try {
            String metaMetaModelPath = "src/main/resources/meta-meta-model.xsd";
            String metaModelPath = "src/main/resources/meta-model-university.xml";
            String dataXMLPath = "src/main/resources/university.xml";

            if (!XMLValidator.validateXMLSchema(metaMetaModelPath, metaModelPath)) {
                System.out.println("Validation failed: meta-model.xsd is not valid against meta-meta-model.xsd");
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
