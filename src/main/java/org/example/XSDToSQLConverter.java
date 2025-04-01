package org.example;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XSDToSQLConverter {
    public static List<String> parseXSD(String xsdFilePath) {
        List<String> tableDefinitions = new ArrayList<>();
        List<String> foreignKeys = new ArrayList<>();

        try {
            File xsdFile = new File(xsdFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xsdFile);

            NodeList entities = document.getElementsByTagName("Enitity");

            for (int i = 0; i < entities.getLength(); i++) {
                Element entity = (Element) entities.item(i);
                String tableName = entity.getAttribute("Name");

                StringBuilder createTableSQL = new StringBuilder("CREATE TABLE " + tableName + " (");
                createTableSQL.append("id INT AUTO_INCREMENT PRIMARY KEY, ");

                NodeList attributes = entity.getElementsByTagName("attribute");
                for (int j = 0; j < attributes.getLength(); j++) {
                    Element attribute = (Element) attributes.item(j);
                    String fieldName = attribute.getAttribute("Name");
                    String fieldType = mapXSDTypeToSQL(attribute.getAttribute("type"));

                    createTableSQL.append(fieldName).append(" ").append(fieldType).append(", ");
                }

                // Get relations (Foreign Keys)
                NodeList relations = entity.getElementsByTagName("relation");
                for (int j = 0; j < relations.getLength(); j++) {
                    Element relation = (Element) relations.item(j);
                    String relationName = relation.getAttribute("Name"); // Relation Name
                    String targetTable = relation.getAttribute("target"); // Target Table

                    // Create foreign key column
                    String foreignKeyColumn = relationName + "_id";
                    createTableSQL.append(foreignKeyColumn).append(" INT, ");

                    // Store the foreign key constraint separately
                    String fkConstraint = "ALTER TABLE " + tableName +
                            " ADD CONSTRAINT fk_" + tableName + "_" + targetTable +
                            " FOREIGN KEY (" + foreignKeyColumn + ") REFERENCES " + targetTable + "(id);";
                    foreignKeys.add(fkConstraint);
                }

                // Remove last comma and finalize table creation SQL
                createTableSQL.setLength(createTableSQL.length() - 2);
                createTableSQL.append(");");

                tableDefinitions.add(createTableSQL.toString());
            }

            // Print debug statements
            System.out.println("Generated SQL Statements:");
            for (String sql : tableDefinitions) System.out.println(sql);
            for (String fk : foreignKeys) System.out.println(fk);

            // Add foreign key constraints at the end
            tableDefinitions.addAll(foreignKeys);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableDefinitions;
    }

    private static String mapXSDTypeToSQL(String xsdType) {
        return switch (xsdType.toLowerCase()) {
            case "string" -> "VARCHAR(255)";
            case "int", "integer" -> "INT";
            case "float", "double" -> "DOUBLE";
            case "boolean" -> "BOOLEAN";
            case "date" -> "DATE";
            default -> "TEXT";
        };
    }
}