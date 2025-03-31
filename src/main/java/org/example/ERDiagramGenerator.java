package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ERDiagramGenerator {
    public static void generateERDiagram(List<String> tableDefinitions, String outputFilePath) {
        // Ensure the output directory exists
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs(); // Create the directory if it doesn't exist
        }

        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write("digraph ERDiagram {\n");
            writer.write("  node [shape=box, style=filled, fillcolor=lightblue];\n");

            for (String sql : tableDefinitions) {
                if (sql.startsWith("CREATE TABLE")) {
                    String tableName = sql.split(" ")[2]; // Extract table name
                    writer.write("  " + tableName + ";\n");
                } else if (sql.startsWith("ALTER TABLE")) {
                    String[] parts = sql.split(" ");
                    String sourceTable = parts[2];
                    String targetTable = parts[parts.length - 1].replace("(id);", "");
                    writer.write("  " + sourceTable + " -> " + targetTable + " [label=\"Foreign Key\"];\n");
                }
            }

            writer.write("}");
            System.out.println("ER Diagram DOT file generated: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
