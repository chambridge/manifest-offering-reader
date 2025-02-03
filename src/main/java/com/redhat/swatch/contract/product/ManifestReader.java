package com.redhat.swatch.contract.product;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import javax.json.*;


public class ManifestReader {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ManifestReader <zip-file-path>");
        }
        String zipFilePath = args[0];
        List<HashMap<Object, Object>> products = processManifestZip(zipFilePath);
        Iterator pIter = products.iterator();
        while (pIter.hasNext()) {
            System.out.println(pIter.next());
        }
    }

    private static List<HashMap<Object, Object>> processManifestZip(String zipFilePath) {
        List<HashMap<Object, Object>> products = null;
        try(ZipInputStream manifestZipStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry manifestEntry;
            while((manifestEntry = manifestZipStream.getNextEntry()) != null) {
                if (manifestEntry.getName().equals("consumer_export.zip")) {
                    products = processConsumerExportZip(new ByteArrayInputStream(readZipEntry(manifestZipStream)));
                }
                manifestZipStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return products;
    }

    private static List<HashMap<Object, Object>> processConsumerExportZip(InputStream consumerExportStream) {
        List<HashMap<Object, Object>> products = new ArrayList<HashMap<Object, Object>>();
        try (ZipInputStream consumerExportZip = new ZipInputStream(consumerExportStream)) {
            ZipEntry exportEntry;
            while ((exportEntry = consumerExportZip.getNextEntry()) != null) {
                if(exportEntry.getName().startsWith("export/entitlements") && exportEntry.getName().endsWith(".json")) {
                    products.add(processEntitlementforCapacity(new ByteArrayInputStream(readZipEntry(consumerExportZip)), exportEntry.getName(), exportEntry.getSize()));
                }
                consumerExportZip.closeEntry();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return products;
    }

    private static byte[] readZipEntry(ZipInputStream zipStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        while((bytesRead = zipStream.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private static HashMap<Object, Object> processEntitlementforCapacity(InputStream entitlementsStream, String fileName, long size) {
        System.out.println("Processing JSON file: " + fileName);
        StringBuilder builder = new StringBuilder();
        JsonObject jsonObject = null;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(entitlementsStream))) {
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        String result = builder.toString().trim();
        try(JsonReader reader = Json.createReader(new StringReader(result))) {
            jsonObject = reader.readObject();
        }

        HashMap<Object, Object> product = new HashMap<Object, Object>();
        JsonObject pool = jsonObject.getJsonObject("pool");
        int quantity = pool.getInt("quantity", 0);
        product.put("productId", pool.getString("productId"));
        product.put("productName", pool.getString("productName"));
        product.put("quantity", quantity);
        JsonArray productAttributes = pool.getJsonArray("productAttributes");
        Iterator<JsonValue> productAttributesIter = productAttributes.iterator();
        while(productAttributesIter.hasNext()) {
            JsonValue productAttr = productAttributesIter.next();
            JsonObject productAttrObject = null;
            try(JsonReader reader = Json.createReader(new StringReader(productAttr.toString()))) {
                productAttrObject = reader.readObject();
            }
            String productAttrName = productAttrObject.getString("name");
            switch (productAttrName) {
                case "role":
                    product.put("role", productAttrObject.getString("value"));
                    break;
                case "description":
                    product.put("description", productAttrObject.getString("value"));
                    break;
                case "usage":
                    product.put("usage", productAttrObject.getString("value"));
                    break;
                case "service_type":
                    product.put("service_type", productAttrObject.getString("value"));
                    break;
                case "product_family":
                    product.put("product_family", productAttrObject.getString("value"));
                    break;
                case "sockets":
                    int sockets = 0;
                    String socketsStr = productAttrObject.getString("value");
                    try {                    
                        sockets = Integer.parseInt(socketsStr);
                    } catch (NumberFormatException e) {
                        sockets = 0;
                    }
                    product.put("sockets", new Integer(sockets * quantity));
                    break;
                case "cores":
                    int cores = 0;
                    String coresStr = productAttrObject.getString("value");
                    try {                    
                        cores = Integer.parseInt(coresStr);
                    } catch (NumberFormatException e) {
                        cores = 0;
                    }
                    product.put("cores", new Integer(cores * quantity));
                    break;
                case "support_type":
                    String support = productAttrObject.getString("value");
                    product.put("L1", new Boolean(support.contains("L1")));
                    product.put("L2", new Boolean(support.contains("L1-L3") || support.contains("L2")));
                    break;
                default:
                    break;
            }
        }

        //System.out.println(product);
        return product;
    }
}