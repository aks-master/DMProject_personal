package org.example;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class XSDToSQLConverter {
    public static List<String> generateTablesFromXSD(String xsdFilePath) {
        List<String> tableDefinitions = new ArrayList<>();
        try {
            File xsdFile = new File(xsdFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xsdFile);

            NodeList entities = document.getElementsByTagName("Enitity");

            for (int i = 0; i < entities.getLength(); i++) {
                Element entity = (Element) entities.item(i);
                String entityName = entity.getAttribute("Name");

                StringBuilder createTableSQL = new StringBuilder("CREATE TABLE " + entityName + " (");
                createTableSQL.append("id INT AUTO_INCREMENT PRIMARY KEY, ");

                NodeList attributes = entity.getElementsByTagName("attribute");
                for (int j = 0; j < attributes.getLength(); j++) {
                    Element attribute = (Element) attributes.item(j);
                    String attributeName = attribute.getAttribute("Name");
                    String attributeType = attribute.getAttribute("type");
                    createTableSQL.append(attributeName).append(" ").append(mapXSDTypeToSQL(attributeType)).append(", ");
                }

                // Add foreign keys from relations
                NodeList relations = entity.getElementsByTagName("relation");
                for (int j = 0; j < relations.getLength(); j++) {
                    Element relation = (Element) relations.item(j);
                    String relationName = relation.getAttribute("Name");
                    String targetEntity = relation.getAttribute("target");
                    createTableSQL.append(relationName).append("_id INT, ");
                    createTableSQL.append("FOREIGN KEY (" + relationName + "_id) REFERENCES " + targetEntity + "(id), ");
                }

                createTableSQL.setLength(createTableSQL.length() - 2); // Remove last comma
                createTableSQL.append(");");
                tableDefinitions.add(createTableSQL.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableDefinitions;
    }

    private static String mapXSDTypeToSQL(String xsdType) {
        return switch (xsdType) {
            case "string" -> "VARCHAR(255)";
            case "int" -> "INT";
            case "date" -> "DATE";
            case "boolean" -> "BOOLEAN";
            default -> "TEXT";
        };
    }
}
