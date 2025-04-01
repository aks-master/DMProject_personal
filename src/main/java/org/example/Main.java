package org.example;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.*;
public class Main {
    public static void main(String[] args) {

        // Paths to the XSD files
        String metaMetaModelPath = "src/main/resources/meta-meta-model.xsd";
        String metaModelPath = "src/main/resources/meta-model.xsd";

        // Validate meta-model.xsd against meta-meta-model.xsd
        if (validateXMLSchema(metaMetaModelPath, metaModelPath)) {
            System.out.println("Validation successful: meta-model.xsd is valid against meta-meta-model.xsd");
        } else {
            System.out.println("Validation failed: meta-model.xsd is not valid against meta-meta-model.xsd");
            return;
        }

        List<String> sqlStatements = XSDToSQLConverter.parseXSD(metaModelPath);

        if (!sqlStatements.isEmpty()) {
            DatabaseConnector.executeSQL(sqlStatements);
        } else {
            System.out.println("No SQL statements were generated.");
        }
    }

    public static boolean validateXMLSchema(String xsdPath, String xmlPath) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(xsdPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlPath)));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}