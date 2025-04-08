package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.sql.*;
import java.util.*;

public class XMLDataInserter {

    public static void insertStoreData(String xmlFilePath) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(xmlFilePath));

            Map<String, String> relationMap = getRelationMap(conn);
            insertEntity(doc.getDocumentElement(), conn, -1, relationMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> getRelationMap(Connection conn) throws SQLException {
        Map<String, String> map = new HashMap<>();
        String query = "SELECT parent_entity, child_entity, relation_name FROM entity_hierarchy";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                map.put(rs.getString("parent_entity") + "-" + rs.getString("child_entity"),
                        rs.getString("relation_name"));
            }
        }
        return map;
    }

    private static int insertEntity(Element element, Connection conn, int parentId, Map<String, String> relationMap) throws SQLException {
        String tag = element.getTagName();
        NamedNodeMap attrs = element.getAttributes();

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tag).append(" (");
        StringBuilder placeholders = new StringBuilder(" VALUES (");
        List<String> values = new ArrayList<>();

        for (int i = 0; i < attrs.getLength(); i++) {
            sql.append(attrs.item(i).getNodeName()).append(", ");
            placeholders.append("?, ");
            values.add(attrs.item(i).getNodeValue());
        }

        if (!values.isEmpty()) {
            sql.setLength(sql.length() - 2);
            placeholders.setLength(placeholders.length() - 2);
        }
        sql.append(")").append(placeholders).append(")");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setString(i + 1, values.get(i));
            }
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int currentId = rs.getInt(1);

                if (parentId != -1) {
                    String parentTag = ((Element) element.getParentNode()).getTagName();
                    String relationName = relationMap.get(parentTag + "-" + tag);
                    if (relationName == null) {
                        throw new SQLException("Hierarchy violation: No relation from " + parentTag + " to " + tag);
                    }
                    insertRelation(conn, parentTag, tag, relationName, parentId, currentId);
                }

                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    if (children.item(i) instanceof Element) {
                        insertEntity((Element) children.item(i), conn, currentId, relationMap);
                    }
                }

                return currentId;
            }
        }
        return -1;
    }

    private static void insertRelation(Connection conn, String parent, String child, String relationName, int parentId, int childId) throws SQLException {
        String table = "relation_" + parent + "_" + relationName;
        String sql = "INSERT INTO " + table + " (" + parent + "_id, " + child + "_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, parentId);
            stmt.setInt(2, childId);
            stmt.executeUpdate();
        }
    }
}
