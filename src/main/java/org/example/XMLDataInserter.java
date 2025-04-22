package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.sql.*;
import java.util.*;

public class XMLDataInserter {
    public static void insertStoreData(String xmlFilePath) {
        try {
            System.out.println("Parsing XML data from " + xmlFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(xmlFilePath));
            Element root = document.getDocumentElement();

            try (Connection conn = DatabaseConnector.getConnection()) {
                // Load hierarchy rules from the database
                Map<String, Set<String>> hierarchyRules = loadHierarchyRules(conn);

                // Validate the XML structure against the hierarchy rules
                if (!validateXmlHierarchy(root, hierarchyRules)) {
                    System.out.println("XML hierarchy validation failed. Data will not be inserted.");
                    return;
                }

                System.out.println("XML hierarchy validation successful. Proceeding with data insertion.");

                // Process and insert the XML data
                Map<String, Integer> contextMap = new HashMap<>();
                processElement(conn, root, contextMap, hierarchyRules);

                System.out.println("Data insertion completed successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Set<String>> loadHierarchyRules(Connection conn) throws SQLException {
        Map<String, Set<String>> hierarchyRules = new HashMap<>();

        String query = "SELECT parent_entity, child_entity FROM entity_hierarchy";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String parent = rs.getString("parent_entity");
                String child = rs.getString("child_entity");

                hierarchyRules.computeIfAbsent(parent, k -> new HashSet<>()).add(child);
            }
        }

        System.out.println("Loaded hierarchy rules: " + hierarchyRules);
        return hierarchyRules;
    }

    private static boolean validateXmlHierarchy(Element element, Map<String, Set<String>> hierarchyRules) {
        String tag = element.getTagName();

        // Get child elements
        NodeList childNodes = element.getChildNodes();
        List<Element> childElements = new ArrayList<>();

        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                childElements.add((Element) childNodes.item(i));
            }
        }

        // Check each child element against hierarchy rules
        for (Element child : childElements) {
            String childTag = child.getTagName();

            // Check if this parent-child relationship is allowed
            if (!hierarchyRules.containsKey(tag) || !hierarchyRules.get(tag).contains(childTag)) {
                System.out.println("Hierarchy violation: " + childTag + " cannot be a child of " + tag);
                return false;
            }

            // Recursively validate child's hierarchy
            if (!validateXmlHierarchy(child, hierarchyRules)) {
                return false;
            }
        }

        return true;
    }

    private static void processElement(Connection conn, Element element, Map<String, Integer> parentContext,
                                       Map<String, Set<String>> hierarchyRules) throws SQLException {
        String tag = element.getTagName();

        // Get attributes
        Map<String, String> attributeMap = new HashMap<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            attributeMap.put(attr.getNodeName(), attr.getNodeValue());
        }

        // Get or insert entity
        int entityId = getOrInsertEntity(conn, tag, attributeMap);
        if (entityId > 0) {
            // Create new context with this entity
            Map<String, Integer> currentContext = new HashMap<>(parentContext);
            currentContext.put(tag, entityId);

            // Check if this is a leaf entity
            boolean isLeaf = isLeafEntity(tag, hierarchyRules);
            if (isLeaf) {
                getOrInsertRelationship(conn, currentContext);
            }

            // Process child elements
            NodeList childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    processElement(conn, (Element) childNodes.item(i), currentContext, hierarchyRules);
                }
            }
        }
    }

    private static boolean isLeafEntity(String entityName, Map<String, Set<String>> hierarchyRules) {
        // An entity is a leaf if it's not a parent in any rule
        // or if it only has empty children sets
        if (!hierarchyRules.containsKey(entityName)) {
            return true;
        }

        Set<String> children = hierarchyRules.get(entityName);
        return children == null || children.isEmpty();
    }

    private static int getOrInsertEntity(Connection conn, String entityType, Map<String, String> attributeMap) throws SQLException {
        if (attributeMap.isEmpty()) {
            return -1;
        }

        // Get table columns
        List<String> tableColumns = getTableColumns(conn, entityType);
        if (tableColumns.isEmpty()) {
            throw new SQLException("Table " + entityType + " does not exist or has no columns");
        }

        // First, check if entity already exists
        int existingId = findExistingEntity(conn, entityType, attributeMap, tableColumns);
        if (existingId > 0) {
            System.out.println("Found existing " + entityType + " with id: " + existingId);
            return existingId;
        }

        // If not, insert new entity
        return insertEntity(conn, entityType, attributeMap, tableColumns);
    }

    private static int findExistingEntity(Connection conn, String entityType, Map<String, String> attributeMap,
                                          List<String> tableColumns) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id FROM " + entityType + " WHERE ");
        List<String> conditions = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (String column : tableColumns) {
            if (column.equals("id")) continue; // Skip ID column

            for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
                if (column.equals(entry.getKey())) {
                    conditions.add(column + " = ?");
                    values.add(entry.getValue());
                    break;
                }
            }
        }

        if (conditions.isEmpty()) {
            return -1;
        }

        sql.append(String.join(" AND ", conditions));

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setString(i + 1, values.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        return -1; // Not found
    }

    private static int insertEntity(Connection conn, String entityType, Map<String, String> attributeMap,
                                    List<String> tableColumns) throws SQLException {
        // Match attributes to columns
        StringBuilder sql = new StringBuilder("INSERT INTO " + entityType + " (");
        StringBuilder placeholders = new StringBuilder(") VALUES (");
        List<String> values = new ArrayList<>();

        for (String column : tableColumns) {
            if (column.equals("id")) continue; // Skip ID column

            for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
                if (column.equals(entry.getKey())) {
                    sql.append(column).append(", ");
                    placeholders.append("?, ");
                    values.add(entry.getValue());
                    break;
                }
            }
        }

        if (values.isEmpty()) {
            return -1;
        }

        // Remove trailing commas
        sql.setLength(sql.length() - 2);
        placeholders.setLength(placeholders.length() - 2);

        // Complete SQL statement
        String insertSql = sql.toString() + placeholders.toString() + ")";

        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setString(i + 1, values.get(i));
            }

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        System.out.println("Inserted new " + entityType + ": " + attributeMap);
                        return id;
                    }
                }
            }
        }

        return -1;
    }

    private static List<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + tableName);
            while (rs.next()) {
                columns.add(rs.getString("Field"));
            }
        }
        return columns;
    }

    private static void getOrInsertRelationship(Connection conn, Map<String, Integer> contextMap) throws SQLException {
        // Get database name
        String dbName = DatabaseConnector.getDatabaseName();

        // Determine leaf entity by querying the entity_hierarchy table
        String leafEntity = null;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT child_entity FROM entity_hierarchy " +
                     "WHERE child_entity NOT IN (SELECT parent_entity FROM entity_hierarchy)")) {
            if (rs.next()) {
                leafEntity = rs.getString("child_entity");
            }
        } catch (SQLException e) {
            System.out.println("Error finding leaf entity: " + e.getMessage());
            return;
        }

        if (leafEntity == null) {
            System.out.println("Could not determine leaf entity for relationships");
            return;
        }

        if (!contextMap.containsKey(leafEntity)) {
            return; // Can't find leaf entity in context
        }

        // Build the relationship table and columns
        String relationshipTable = leafEntity + "_relationships";
        List<String> columns = getTableColumns(conn, relationshipTable);
        Map<String, Integer> columnValues = new HashMap<>();

        for (Map.Entry<String, Integer> entry : contextMap.entrySet()) {
            String columnName = entry.getKey() + "_id";
            if (columns.contains(columnName)) {
                columnValues.put(columnName, entry.getValue());
            }
        }

        if (columnValues.isEmpty()) {
            return;
        }

        // Check if relationship already exists
        if (relationshipExists(conn, relationshipTable, columnValues)) {
            System.out.println("Relationship already exists for " + leafEntity + " id: " + contextMap.get(leafEntity));
            return;
        }

        // Insert new relationship
        insertRelationship(conn, relationshipTable, columnValues);
    }

    private static boolean relationshipExists(Connection conn, String table, Map<String, Integer> columnValues)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id FROM " + table + " WHERE ");
        List<String> conditions = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : columnValues.entrySet()) {
            conditions.add(entry.getKey() + " = ?");
        }

        sql.append(String.join(" AND ", conditions));

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int index = 1;
            for (Integer value : columnValues.values()) {
                stmt.setInt(index++, value);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Returns true if relationship exists
            }
        }
    }

    private static void insertRelationship(Connection conn, String table, Map<String, Integer> columnValues)
            throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (");
        StringBuilder valuesClause = new StringBuilder(") VALUES (");
        List<Integer> valuesList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : columnValues.entrySet()) {
            sql.append(entry.getKey()).append(", ");
            valuesClause.append("?, ");
            valuesList.add(entry.getValue());
        }

        // Remove trailing commas
        sql.setLength(sql.length() - 2);
        valuesClause.setLength(valuesClause.length() - 2);

        // Complete SQL statement
        String insertSql = sql.toString() + valuesClause.toString() + ")";

        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (int i = 0; i < valuesList.size(); i++) {
                stmt.setInt(i + 1, valuesList.get(i));
            }
            stmt.executeUpdate();
            System.out.println("Inserted new relationship in " + table);
        }
    }
}