package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class XSDToSQLConverter {
    public static List<String> parseXSD(String xsdFilePath) {
        List<String> tableDefinitions = new ArrayList<>();
        List<String> relationTables = new ArrayList<>();
        List<String> hierarchyInserts = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(xsdFilePath));

            NodeList entities = document.getElementsByTagName("Enitity");

            for (int i = 0; i < entities.getLength(); i++) {
                Element entity = (Element) entities.item(i);
                String tableName = entity.getAttribute("Name");

                StringBuilder tableSQL = new StringBuilder("CREATE TABLE " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, ");
                NodeList attributes = entity.getElementsByTagName("attribute");

                for (int j = 0; j < attributes.getLength(); j++) {
                    Element attr = (Element) attributes.item(j);
                    tableSQL.append(attr.getAttribute("Name")).append(" ")
                            .append(mapXSDTypeToSQL(attr.getAttribute("type"))).append(", ");
                }

                if (attributes.getLength() > 0)
                    tableSQL.setLength(tableSQL.length() - 2);
                else
                    tableSQL.setLength(tableSQL.length() - 1);

                tableSQL.append(");");
                tableDefinitions.add(tableSQL.toString());
            }

            for (int i = 0; i < entities.getLength(); i++) {
                Element entity = (Element) entities.item(i);
                String source = entity.getAttribute("Name");
                NodeList relations = entity.getElementsByTagName("relation");

                for (int j = 0; j < relations.getLength(); j++) {
                    Element relation = (Element) relations.item(j);
                    String target = relation.getAttribute("target");
                    String relName = relation.getAttribute("Name");

                    String relTable = "relation_" + source + "_" + relName;
                    String relSQL = "CREATE TABLE " + relTable + " (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            source + "_id INT, " + target + "_id INT, " +
                            "FOREIGN KEY (" + source + "_id) REFERENCES " + source + "(id), " +
                            "FOREIGN KEY (" + target + "_id) REFERENCES " + target + "(id)" +
                            ");";
                    relationTables.add(relSQL);

                    hierarchyInserts.add("INSERT INTO entity_hierarchy (parent_entity, child_entity, relation_name) VALUES " +
                            "('" + source + "', '" + target + "', '" + relName + "');");
                }
            }

            tableDefinitions.add("CREATE TABLE entity_hierarchy (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "parent_entity VARCHAR(255), child_entity VARCHAR(255), relation_name VARCHAR(255));");

            tableDefinitions.addAll(relationTables);
            tableDefinitions.addAll(hierarchyInserts);

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
