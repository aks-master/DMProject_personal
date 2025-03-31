package org.example;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Define the path to the XSD file
        String xsdPath = "src/main/resources/meta-model.xsd"; // Ensure the file exists at this location

        // Parse the XSD and generate SQL statements
        List<String> sqlStatements = XSDToSQLConverter.parseXSD(xsdPath);

        // Execute the SQL statements in MySQL
        if (!sqlStatements.isEmpty()) {
            DatabaseConnector.executeSQL(sqlStatements);
        } else {
            System.out.println("No SQL statements were generated.");
        }
    }
}
