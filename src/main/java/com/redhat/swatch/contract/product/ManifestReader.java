package com.redhat.swatch.contract.product;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ManifestReader {
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        JsonNode jsonObject = null;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(entitlementsStream))) {
            String line;
            while((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        String result = builder.toString().trim();
        try {
            jsonObject = objectMapper.readTree(result);
        } catch(IOException e) {
            jsonObject = null;
        }

        HashMap<Object, Object> product = new HashMap<Object, Object>();

        JsonNode pool = jsonObject.get("pool");
        int quantity = pool.get("exported").intValue();
        product.put("productId", pool.get("productId").asText());
        product.put("productName", pool.get("productName").asText());
        product.put("quantity", quantity);
        JsonNode productAttributes = pool.get("productAttributes");
        if (productAttributes != null && productAttributes.isArray()) {
            for (JsonNode productAttrObject: productAttributes) {
                String productAttrName = productAttrObject.get("name").asText();
                switch (productAttrName) {
                    case "role":
                        product.put("role", productAttrObject.get("value").asText());
                        break;
                    case "description":
                        product.put("description", productAttrObject.get("value").asText());
                        break;
                    case "usage":
                        product.put("usage", productAttrObject.get("value").asText());
                        break;
                    case "service_type":
                        product.put("service_type", productAttrObject.get("value").asText());
                        break;
                    case "product_family":
                        product.put("product_family", productAttrObject.get("value").asText());
                        break;
                    case "sockets":
                        int sockets = 0;
                        try {                    
                            sockets = productAttrObject.get("value").intValue();
                        } catch (NumberFormatException e) {
                            sockets = 0;
                        }
                        product.put("sockets", new Integer(sockets * quantity));
                        break;
                    case "cores":
                        int cores = 0;
                        try {                    
                            cores = productAttrObject.get("value").intValue();
                        } catch (NumberFormatException e) {
                            cores = 0;
                        }
                        product.put("cores", new Integer(cores * quantity));
                        break;
                    case "support_type":
                        String support = productAttrObject.get("value").asText();
                        product.put("L1", new Boolean(support.contains("L1")));
                        product.put("L2", new Boolean(support.contains("L1-L3") || support.contains("L2")));
                        break;
                    default:
                        break;
                }
            }
        }
        return product;
    }
}