package org.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String metaMetaModelPath = "src/main/resources/meta-meta-model.xsd";
        String metaModelPath = "src/main/resources/meta-model.xsd";
        String xmlPath = "src/main/resources/store.xml";

        if (!XMLValidator.validateXMLSchema(metaMetaModelPath, metaModelPath)) {
            System.out.println("Validation failed: meta-model.xsd is not valid against meta-meta-model.xsd");
            return;
        }

        System.out.println("Validation successful.");

        List<String> sqlStatements = XSDToSQLConverter.parseXSD(metaModelPath);
        if (sqlStatements.isEmpty()) {
            System.out.println("No SQL statements were generated.");
            return;
        }

        DatabaseConnector.executeSQL(sqlStatements);
        XMLDataInserter.insertStoreData(xmlPath);
        DataRetriever.displayInsertedTables();
    }
}
