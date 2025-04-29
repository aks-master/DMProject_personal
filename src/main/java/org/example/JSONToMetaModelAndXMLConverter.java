package org.example;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class JSONToMetaModelAndXMLConverter {

    public static void convert() {
        try (Scanner scanner = new Scanner(System.in)) {
            // Prompt the user for the JSON file path
            System.out.print("Enter the path to the JSON file: ");
            String jsonFilePath = scanner.nextLine();

            // Read the JSON file
            String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            JSONObject json = new JSONObject(jsonContent);

            String fileName = new File(jsonFilePath).getName().replace(".json", "");

            // Create a set to keep track of entities already processed
            Set<String> seen = new HashSet<>();

            // Generate meta-model XML
            StringBuilder metaSb = new StringBuilder();
            metaSb.append("<?xml version=\"1.0\"?>\n")
                    .append("<meta-meta-model xmlns=\"http://iiitb.ac.in/mt2024111\"\n")
                    .append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
                    .append("    xsi:schemaLocation=\"http://iiitb.ac.in/mt2024111 meta-meta-model.xsd\">\n");

            // Recursively build the meta-model XML
            buildMetaModelEntity(json, metaSb, seen);

            metaSb.append("</meta-meta-model>");

            // Generate data XML
            String dataXML = generateDataXML(json);

            // Save the generated files
            try (FileWriter metaWriter = new FileWriter("src/main/resources/meta-model-" + fileName + ".xml")) {
                metaWriter.write(metaSb.toString());
            }

            try (FileWriter dataWriter = new FileWriter("src/main/resources/" + fileName + ".xml")) {
                dataWriter.write(dataXML);
            }

            System.out.println("Conversion successful: meta-model-" + fileName + ".xml and " + fileName + ".xml created.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to recursively generate the meta-model XML
    private static void buildMetaModelEntity(Object node, StringBuilder sb, Set<String> seen) {
        if (node instanceof JSONObject obj) {
            for (String key : obj.keySet()) {
                Object val = obj.get(key);

                // Only emit entity once per name
                if ((val instanceof JSONObject || val instanceof JSONArray) && seen.add(key)) {
                    sb.append("    <Enitity Name=\"").append(key).append("\">\n");

                    // Inspect a sample object to distinguish attributes vs relations
                    JSONObject sample = null;
                    if (val instanceof JSONObject) {
                        sample = (JSONObject) val;
                    } else {
                        JSONArray arr = (JSONArray) val;
                        if (arr.length() > 0 && arr.get(0) instanceof JSONObject) {
                            sample = arr.getJSONObject(0);
                        }
                    }

                    // Add attributes and relations
                    if (sample != null) {
                        for (String subKey : sample.keySet()) {
                            Object subVal = sample.get(subKey);
                            if (subVal instanceof JSONObject || subVal instanceof JSONArray) {
                                sb.append("        <relation Name=\"Has")
                                        .append(capitalize(subKey))
                                        .append("\" target=\"")
                                        .append(subKey)
                                        .append("\"/>\n");
                            } else {
                                sb.append("        <attribute Name=\"")
                                        .append(subKey)
                                        .append("\" type=\"string\"/>\n");
                            }
                        }
                    }

                    sb.append("    </Enitity>\n");
                }

                // Recurse into nested objects/arrays
                if (val instanceof JSONObject || val instanceof JSONArray) {
                    buildMetaModelEntity(val, sb, seen);
                }
            }
        } else if (node instanceof JSONArray arr) {
            for (int i = 0; i < arr.length(); i++) {
                buildMetaModelEntity(arr.get(i), sb, seen);
            }
        }
    }

    // Method to generate the data XML based on the JSON structure
    private static String generateDataXML(JSONObject json) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");

        // Iterate over the root elements of the JSON
        for (String root : json.keySet()) {
            sb.append("<").append(root);
            Object rootObj = json.get(root);
            if (rootObj instanceof JSONObject) {
                JSONObject rootJson = (JSONObject) rootObj;
                // Write attributes
                for (String k : rootJson.keySet()) {
                    Object val = rootJson.get(k);
                    if (!(val instanceof JSONArray)) {
                        sb.append(" ").append(k).append("=\"").append(val.toString()).append("\"");
                    }
                }
                sb.append(">\n");
                // Recursively write nested data
                writeDataRecursive(rootJson, sb);
                sb.append("</").append(root).append(">");
            }
        }
        return sb.toString();
    }

    // Recursive method to handle nested data in JSON
    private static void writeDataRecursive(JSONObject json, StringBuilder sb) {
        for (String key : json.keySet()) {
            Object val = json.get(key);
            if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject child = arr.getJSONObject(i);
                    sb.append("  <").append(key);
                    for (String attr : child.keySet()) {
                        Object attrVal = child.get(attr);
                        if (!(attrVal instanceof JSONArray)) {
                            sb.append(" ").append(attr).append("=\"").append(attrVal.toString()).append("\"");
                        }
                    }
                    sb.append(">\n");
                    writeDataRecursive(child, sb);
                    sb.append("  </").append(key).append(">\n");
                }
            }
        }
    }

    // Helper method to capitalize the first letter of a string
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}