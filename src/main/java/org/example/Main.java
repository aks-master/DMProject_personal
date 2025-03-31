package org.example;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Path to the XSD file
        String xsdFilePath = "src/main/resources/meta-model.xsd";

        // Generate the table definitions from the XSD
        List<String> tableDefinitions = XSDToSQLConverter.generateTablesFromXSD(xsdFilePath);

        // Create the tables in the database
        DatabaseConnector.executeTableCreationSQL(tableDefinitions);

        // Path to the XML file containing data to insert
        String xmlFilePath = "src/main/resources/store.xml";

        // Parse the XML and generate data to insert
        List<Category> categories = parseXMLAndGenerateData(xmlFilePath);

        // Insert the parsed data into the database
        DatabaseConnector.insertData("abc", categories);  // "abc" is the store name
    }

    public static List<Category> parseXMLAndGenerateData(String xmlFilePath) {
        List<Category> categories = new ArrayList<>();
        try {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            NodeList storeNodes = document.getElementsByTagName("store");

            for (int i = 0; i < storeNodes.getLength(); i++) {
                Element storeElement = (Element) storeNodes.item(i);

                NodeList categoryNodes = storeElement.getElementsByTagName("category");
                List<Category> categoryList = new ArrayList<>();

                for (int j = 0; j < categoryNodes.getLength(); j++) {
                    Element categoryElement = (Element) categoryNodes.item(j);
                    String categoryName = categoryElement.getAttribute("category_name");

                    NodeList subcategoryNodes = categoryElement.getElementsByTagName("subcategory");
                    List<Subcategory> subcategoryList = new ArrayList<>();

                    for (int k = 0; k < subcategoryNodes.getLength(); k++) {
                        Element subcategoryElement = (Element) subcategoryNodes.item(k);
                        String subcategoryName = subcategoryElement.getAttribute("subcategory_name");

                        NodeList productNodes = subcategoryElement.getElementsByTagName("product");
                        List<Product> productList = new ArrayList<>();

                        for (int l = 0; l < productNodes.getLength(); l++) {
                            Element productElement = (Element) productNodes.item(l);
                            String productName = productElement.getAttribute("product_name");
                            String price = productElement.getAttribute("price");
                            productList.add(new Product(productName, price));
                        }

                        subcategoryList.add(new Subcategory(subcategoryName, productList));
                    }

                    categoryList.add(new Category(categoryName, subcategoryList));
                }

                categories.addAll(categoryList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return categories;
    }
}
