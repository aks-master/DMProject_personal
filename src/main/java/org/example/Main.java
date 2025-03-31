package org.example;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String xsdPath = "src/main/resources/meta-model.xsd";
        String erDiagramPath = "output/er_diagram.dot";

        List<String> sqlStatements = XSDToSQLConverter.parseXSD(xsdPath);

        if (!sqlStatements.isEmpty()) {
            DatabaseConnector.executeSQL(sqlStatements);

            // Generate ER Diagram
//            ERDiagramGenerator.generateERDiagram(sqlStatements, erDiagramPath);

            // Convert DOT file to PNG (Requires Graphviz installed)
//            try {
//                Process process = new ProcessBuilder("dot", "-Tpng", erDiagramPath, "-o", "output/er_diagram.png")
//                        .start();
//                process.waitFor();
//                System.out.println("ER Diagram generated: output/er_diagram.png");
//            } catch (Exception e) {
//                System.out.println("Error generating ER Diagram.");
//                e.printStackTrace();
//            }
        } else {
            System.out.println("No SQL statements were generated.");
        }
    }
}
