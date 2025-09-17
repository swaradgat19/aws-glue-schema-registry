package com.amazonaws.services.schemaregistry.integrationtests.performance;

import com.amazonaws.services.schemaregistry.common.AWSSerializerInput;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistrySerializationFacade;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.glue.model.DataFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.schemaregistry.deserializers.protobuf.ProtobufSchemaParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CleanPerformanceTest {

    private static final String REGION = "us-east-1";
    private static final String REGISTRY_NAME = "default-registry";
    private static final String SCHEMA_NAME = "schema-resolution-test";

    private static GlueSchemaRegistrySerializationFacade facade;
    private static String testSchema;

    @BeforeAll
    static void setup() {
        System.out.println("=== SCHEMA RESOLUTION PERFORMANCE TEST SETUP ===");

        // Configure GSR
        Map<String, Object> configs = new HashMap<>();
        configs.put(AWSSchemaRegistryConstants.AWS_REGION, REGION);
        configs.put(AWSSchemaRegistryConstants.REGISTRY_NAME, REGISTRY_NAME);
        configs.put(AWSSchemaRegistryConstants.SCHEMA_NAME, SCHEMA_NAME);
        configs.put(AWSSchemaRegistryConstants.DATA_FORMAT, DataFormat.JSON.name());
        configs.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, true);

        // Create facade directly
        facade = GlueSchemaRegistrySerializationFacade.builder()
                // .credentialProvider(DefaultCredentialsProvider.create())
                .credentialProvider(DefaultCredentialsProvider.builder().build())
                .configs(configs)
                .build();

        System.out.println("Facade instance after: " + System.identityHashCode(facade));

        // Simple test schema
        testSchema = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\"type\": \"string\"},\n" +
                "    \"message\": {\"type\": \"string\"}\n" +
                "  },\n" +
                "  \"required\": [\"id\", \"message\"]\n" +
                "}";

        System.out.println("✅ Setup complete");
    }

    @Test
    public void testSchemaRegistration() throws Exception {
        System.out.println("\n=== SCHEMA REGISTRATION TEST ===");

        AWSSerializerInput input = AWSSerializerInput.builder()
                .schemaDefinition(testSchema)
                .schemaName(SCHEMA_NAME)
                .dataFormat(DataFormat.JSON.name())
                .transportName("test-topic")
                .build();

        // First call - should register schema
        System.out.println("--- First call (registration) ---");
        long start1 = System.nanoTime();
        UUID schemaVersionId1 = facade.getOrRegisterSchemaVersion(input);
        long end1 = System.nanoTime();

        assertNotNull(schemaVersionId1);
        double duration1 = (end1 - start1) / 1_000_000.0;
        System.out.println("Schema Version ID: " + schemaVersionId1);
        System.out.println("Duration: " + duration1 + " ms");

        // Second call - should use cache
        System.out.println("\n--- Second call (cached) ---");
        long start2 = System.nanoTime();
        UUID schemaVersionId2 = facade.getOrRegisterSchemaVersion(input);
        long end2 = System.nanoTime();

        assertNotNull(schemaVersionId2);
        double duration2 = (end2 - start2) / 1_000_000.0;
        System.out.println("Schema Version ID: " + schemaVersionId2);
        System.out.println("Duration: " + duration2 + " ms");

        // Verify same schema version
        if (schemaVersionId1.equals(schemaVersionId2)) {
            System.out.println("✅ Same schema version returned");
        } else {
            System.out.println("❌ Different schema versions returned");
        }

        // Performance comparison
        System.out.println("\n--- Performance Comparison ---");
        System.out.println("Registration: " + duration1 + " ms");
        System.out.println("Cached lookup: " + duration2 + " ms");
        System.out.println("Speedup: " + String.format("%.1f", duration1 / duration2) + "x faster");

        System.out.println("\n✅ Schema resolution test completed!");
    }

    @Test
    public void testJSONSerializationPerformance() throws Exception {
        System.out.println("\n=== JSON SERIALIZATION PERFORMANCE TEST ===");

        // Test different payload sizes
        testJSONPayloadSize("SMALL", createSmallJsonData(), createSmallJsonSchema());
        testJSONPayloadSize("MEDIUM", createMediumJsonData(), createMediumJsonSchema());
        testJSONPayloadSize("LARGE", createLargeJsonData(), createLargeJsonSchema());

        System.out.println("\n✅ All JSON serialization performance tests completed!");
    }

    private void testJSONPayloadSize(String sizeCategory, String jsonData, String schema) throws Exception {
        System.out.println("\n=== " + sizeCategory + " JSON PAYLOAD TEST ===");

        // Register schema for this payload size
        AWSSerializerInput input = AWSSerializerInput.builder()
                .schemaDefinition(schema)
                .schemaName(SCHEMA_NAME + "-" + sizeCategory.toLowerCase())
                .dataFormat(DataFormat.JSON.name())
                .transportName("test-topic")
                .build();

        UUID schemaVersionId = facade.getOrRegisterSchemaVersion(input);
        System.out.println("Schema registered: " + schemaVersionId);

        // Parse JSON to create proper data object
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Object testData = mapper.readValue(jsonData, Object.class);

        System.out.println("Payload size: " + jsonData.length() + " characters");
        System.out.println("Sample data: " + (jsonData.length() > 100 ? jsonData.substring(0, 100) + "..." : jsonData));

        // === WARMUP PHASE ===
        System.out.println("\n--- Warmup (10 iterations) ---");
        for (int i = 0; i < 10; i++) {
            byte[] serialized = facade.serialize(DataFormat.JSON, testData, schemaVersionId);
            assertNotNull(serialized);
        }
        System.out.println("✅ Warmup complete");

        // === MEASUREMENT PHASE ===
        System.out.println("\n--- Measurement (50,000 iterations) ---");

        long[] serializationTimes = new long[50000];
        long totalTime = 0;

        for (int i = 0; i < 50000; i++) {
            long start = System.nanoTime();
            byte[] serialized = facade.serialize(DataFormat.JSON, testData, schemaVersionId);
            long end = System.nanoTime();

            assertNotNull(serialized);
            serializationTimes[i] = end - start;
            totalTime += serializationTimes[i];

            // Print progress every 5000 iterations
            if ((i + 1) % 5000 == 0) {
                System.out.println("Completed " + (i + 1) + "/50,000 iterations");
            }
        }

        // === STATISTICS ===
        double avgMs = (totalTime / 50000.0) / 1_000_000.0;

        // Calculate percentiles
        java.util.Arrays.sort(serializationTimes);
        double p50Ms = serializationTimes[25000] / 1_000_000.0;
        double p95Ms = serializationTimes[47500] / 1_000_000.0;
        double p99Ms = serializationTimes[49500] / 1_000_000.0;
        double p999Ms = serializationTimes[49950] / 1_000_000.0;
        double minMs = serializationTimes[0] / 1_000_000.0;
        double maxMs = serializationTimes[49999] / 1_000_000.0;

        System.out.println("\n--- " + sizeCategory + " JSON RESULTS ---");
        System.out.println("Payload Size: " + jsonData.length() + " characters");
        System.out.println("Iterations: 50,000");
        System.out.println("Average: " + String.format("%.3f", avgMs) + " ms");
        System.out.println("Median (P50): " + String.format("%.3f", p50Ms) + " ms");
        System.out.println("P95: " + String.format("%.3f", p95Ms) + " ms");
        System.out.println("P99: " + String.format("%.3f", p99Ms) + " ms");
        System.out.println("P99.9: " + String.format("%.3f", p999Ms) + " ms");
        System.out.println("Min: " + String.format("%.3f", minMs) + " ms");
        System.out.println("Max: " + String.format("%.3f", maxMs) + " ms");

        // Calculate throughput
        double totalTimeSeconds = totalTime / 1_000_000_000.0;
        double throughputPerSecond = 50000 / totalTimeSeconds;

        System.out.println("\n--- " + sizeCategory + " THROUGHPUT ---");
        System.out.println("Total time: " + String.format("%.3f", totalTimeSeconds) + " seconds");
        System.out.println("Throughput: " + String.format("%.0f", throughputPerSecond) + " serializations/second");
        System.out.println("Bytes per second: " + String.format("%.0f", throughputPerSecond * jsonData.length())
                + " chars/second");

        // Additional statistics for 50K iterations
        double stdDevMs = calculateStandardDeviation(serializationTimes, avgMs * 1_000_000);
        double coefficientOfVariation = (stdDevMs / avgMs) * 100;

        System.out.println("\n--- EXTENDED STATISTICS ---");
        System.out.println("Standard deviation: " + String.format("%.3f", stdDevMs) + " ms");
        System.out.println("Coefficient of variation: " + String.format("%.1f", coefficientOfVariation) + "%");

        System.out.println("\n✅ " + sizeCategory + " JSON test completed!");
    }

    @Test
    public void testProtobufSerializationPerformance() throws Exception {
        System.out.println("\n=== PROTOBUF SERIALIZATION PERFORMANCE TEST ===");

        // Test different payload sizes
        testProtobufPayloadSize("SMALL", createSmallProtobufMessage());
        testProtobufPayloadSize("MEDIUM", createMediumProtobufMessage());
        testProtobufPayloadSize("LARGE", createLargeProtobufMessage());

        System.out.println("\n✅ All Protobuf serialization performance tests completed!");
    }

    private void testProtobufPayloadSize(String sizeCategory, DynamicMessage testMessage) throws Exception {
        System.out.println("\n=== " + sizeCategory + " PROTOBUF PAYLOAD TEST ===");

        // Configure GSR for Protobuf
        Map<String, Object> configs = new HashMap<>();
        configs.put(AWSSchemaRegistryConstants.AWS_REGION, REGION);
        configs.put(AWSSchemaRegistryConstants.REGISTRY_NAME, REGISTRY_NAME);
        configs.put(AWSSchemaRegistryConstants.SCHEMA_NAME, "protobuf-schema-" + sizeCategory.toLowerCase());
        configs.put(AWSSchemaRegistryConstants.DATA_FORMAT, DataFormat.PROTOBUF.name());
        configs.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, true);

        // Create facade for Protobuf
        GlueSchemaRegistrySerializationFacade protobufFacade = GlueSchemaRegistrySerializationFacade.builder()
                .credentialProvider(DefaultCredentialsProvider.builder().build())
                .configs(configs)
                .build();

        // Get schema from the message
        String protoSchema = getProtobufSchemaFromMessage(testMessage, sizeCategory);

        // Register schema for this payload size
        AWSSerializerInput input = AWSSerializerInput.builder()
                .schemaDefinition(protoSchema)
                .schemaName("protobuf-schema-" + sizeCategory.toLowerCase())
                .dataFormat(DataFormat.PROTOBUF.name())
                .transportName("test-topic")
                .build();

        UUID schemaVersionId = protobufFacade.getOrRegisterSchemaVersion(input);
        System.out.println("Schema registered: " + schemaVersionId);

        // Calculate message size
        byte[] messageBytes = testMessage.toByteArray();
        System.out.println("Message size: " + messageBytes.length + " bytes");
        System.out.println("Sample message: "
                + testMessage.toString().substring(0, Math.min(100, testMessage.toString().length())) + "...");

        // === WARMUP PHASE ===
        System.out.println("\n--- Warmup (10 iterations) ---");
        for (int i = 0; i < 10; i++) {
            byte[] serialized = protobufFacade.serialize(DataFormat.PROTOBUF, testMessage, schemaVersionId);
            assertNotNull(serialized);
        }
        System.out.println("✅ Warmup complete");

        // === MEASUREMENT PHASE ===
        System.out.println("\n--- Measurement (50,000 iterations) ---");

        long[] serializationTimes = new long[50000];
        long totalTime = 0;

        for (int i = 0; i < 50000; i++) {
            long start = System.nanoTime();
            byte[] serialized = protobufFacade.serialize(DataFormat.PROTOBUF, testMessage, schemaVersionId);
            long end = System.nanoTime();

            assertNotNull(serialized);
            serializationTimes[i] = end - start;
            totalTime += serializationTimes[i];

            // Print progress every 5000 iterations
            if ((i + 1) % 5000 == 0) {
                System.out.println("Completed " + (i + 1) + "/50,000 iterations");
            }
        }

        // === STATISTICS ===
        double avgMs = (totalTime / 50000.0) / 1_000_000.0;

        // Calculate percentiles
        java.util.Arrays.sort(serializationTimes);
        double p50Ms = serializationTimes[25000] / 1_000_000.0;
        double p95Ms = serializationTimes[47500] / 1_000_000.0;
        double p99Ms = serializationTimes[49500] / 1_000_000.0;
        double p999Ms = serializationTimes[49950] / 1_000_000.0;
        double minMs = serializationTimes[0] / 1_000_000.0;
        double maxMs = serializationTimes[49999] / 1_000_000.0;

        System.out.println("\n--- " + sizeCategory + " PROTOBUF RESULTS ---");
        System.out.println("Message Size: " + messageBytes.length + " bytes");
        System.out.println("Iterations: 50,000");
        System.out.println("Average: " + String.format("%.3f", avgMs) + " ms");
        System.out.println("Median (P50): " + String.format("%.3f", p50Ms) + " ms");
        System.out.println("P95: " + String.format("%.3f", p95Ms) + " ms");
        System.out.println("P99: " + String.format("%.3f", p99Ms) + " ms");
        System.out.println("P99.9: " + String.format("%.3f", p999Ms) + " ms");
        System.out.println("Min: " + String.format("%.3f", minMs) + " ms");
        System.out.println("Max: " + String.format("%.3f", maxMs) + " ms");

        // Calculate throughput
        double totalTimeSeconds = totalTime / 1_000_000_000.0;
        double throughputPerSecond = 50000 / totalTimeSeconds;

        System.out.println("\n--- " + sizeCategory + " THROUGHPUT ---");
        System.out.println("Total time: " + String.format("%.3f", totalTimeSeconds) + " seconds");
        System.out.println("Throughput: " + String.format("%.0f", throughputPerSecond) + " serializations/second");
        System.out.println("Bytes per second: " + String.format("%.0f", throughputPerSecond * messageBytes.length)
                + " bytes/second");

        // Additional statistics for 50K iterations
        double stdDevMs = calculateStandardDeviation(serializationTimes, avgMs * 1_000_000);
        double coefficientOfVariation = (stdDevMs / avgMs) * 100;

        System.out.println("\n--- EXTENDED STATISTICS ---");
        System.out.println("Standard deviation: " + String.format("%.3f", stdDevMs) + " ms");
        System.out.println("Coefficient of variation: " + String.format("%.1f", coefficientOfVariation) + "%");

        System.out.println("\n✅ " + sizeCategory + " PROTOBUF test completed!");
    }

    private void validateSerializedOutputStructure(byte[] serialized, UUID expectedSchemaVersionId) {
        System.out.println("\n--- SERIALIZED OUTPUT STRUCTURE VALIDATION ---");

        ByteBuffer buffer = ByteBuffer.wrap(serialized);

        // 1. Validate Version Byte (1 byte)
        byte versionByte = buffer.get();
        System.out.println(
                "Version Byte: " + versionByte + " (expected: " + AWSSchemaRegistryConstants.HEADER_VERSION_BYTE + ")");
        assertEquals(AWSSchemaRegistryConstants.HEADER_VERSION_BYTE, versionByte,
                "Version byte should match GSR header version");

        // 2. Validate Compression Byte (1 byte)
        byte compressionByte = buffer.get();
        System.out.println("Compression Byte: " + compressionByte + " (expected: "
                + AWSSchemaRegistryConstants.COMPRESSION_DEFAULT_BYTE + " for no compression)");
        assertEquals(AWSSchemaRegistryConstants.COMPRESSION_DEFAULT_BYTE, compressionByte,
                "Compression byte should indicate no compression by default");

        // 3. Validate Schema Version UUID (16 bytes)
        long mostSignificantBits = buffer.getLong();
        long leastSignificantBits = buffer.getLong();
        UUID extractedSchemaVersionId = new UUID(mostSignificantBits, leastSignificantBits);
        System.out.println("Extracted Schema Version ID: " + extractedSchemaVersionId);
        System.out.println("Expected Schema Version ID:  " + expectedSchemaVersionId);
        assertEquals(expectedSchemaVersionId, extractedSchemaVersionId,
                "Schema Version ID should match the registered schema");

        // 4. Validate remaining bytes are the actual Protobuf data
        int headerSize = 1 + 1 + 16; // version + compression + UUID
        int actualDataSize = serialized.length - headerSize;
        System.out.println("Header Size: " + headerSize + " bytes");
        System.out.println("Actual Protobuf Data Size: " + actualDataSize + " bytes");
        System.out.println("Total Size: " + serialized.length + " bytes");

        // Extract the actual protobuf data
        byte[] protobufData = new byte[actualDataSize];
        buffer.get(protobufData);

        System.out.println(
                "Protobuf Data (first 20 bytes): " + bytesToHex(protobufData, Math.min(20, protobufData.length)));

        // Validate structure breakdown
        System.out.println("\n--- STRUCTURE BREAKDOWN ---");
        System.out.println("Byte 0:      Version Byte = " + versionByte);
        System.out.println("Byte 1:      Compression Byte = " + compressionByte);
        System.out.println("Bytes 2-17:  Schema Version UUID = " + extractedSchemaVersionId);
        System.out.println(
                "Bytes 18-" + (serialized.length - 1) + ": Protobuf Message Data (" + actualDataSize + " bytes)");

        System.out.println("✅ Serialized output structure validation passed!");
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(String.format("%02X ", bytes[i]));
        }
        return result.toString().trim();
    }

    // ===== FILE LOADING METHODS =====

    private String loadPerformanceFile(String format, String filename) throws IOException {
        Path filePath = Paths.get("src/test/resources/performance/" + format + "/" + filename);
        return new String(Files.readAllBytes(filePath));
    }

    private String loadJsonFile(String filename) throws IOException {
        return loadPerformanceFile("json", filename);
    }

    private String loadProtobufSchema(String filename) throws IOException {
        return loadPerformanceFile("protobuf", filename);
    }

    // ===== JSON TEST DATA METHODS =====

    private String createSmallJsonData() throws IOException {
        return loadJsonFile("small-payload.json");
    }

    private String createSmallJsonSchema() throws IOException {
        return loadJsonFile("small-schema.json");
    }

    private String createMediumJsonData() throws IOException {
        return loadJsonFile("medium-payload.json");
    }

    private String createMediumJsonSchema() throws IOException {
        return loadJsonFile("medium-schema.json");
    }

    private String createLargeJsonData() throws IOException {
        return loadJsonFile("large-payload.json");
    }

    private String createLargeJsonSchema() throws IOException {
        return loadJsonFile("large-schema.json");
    }

    private double calculateStandardDeviation(long[] values, double meanNanos) {
        double sumSquaredDiffs = 0.0;
        for (long value : values) {
            double diff = value - meanNanos;
            sumSquaredDiffs += diff * diff;
        }
        double variance = sumSquaredDiffs / values.length;
        return Math.sqrt(variance) / 1_000_000.0; // Convert to milliseconds
    }

    // ===== PROTOBUF TEST DATA METHODS =====

    private DynamicMessage createSmallProtobufMessage() throws Exception {
        String protoSchema = loadProtobufSchema("small-message.proto");
        Descriptors.FileDescriptor fileDescriptor = ProtobufSchemaParser.parse(protoSchema, "small-message.proto");
        Descriptors.Descriptor messageDescriptor = fileDescriptor.findMessageTypeByName("SimpleMessage");

        return DynamicMessage.newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByName("id"), "small-msg-12345")
                .setField(messageDescriptor.findFieldByName("message"), "Small Protobuf performance test message")
                .setField(messageDescriptor.findFieldByName("number"), 42)
                .build();
    }

    private DynamicMessage createMediumProtobufMessage() throws Exception {
        String mediumProtoSchema = loadProtobufSchema("medium-message.proto");
        Descriptors.FileDescriptor fileDescriptor = ProtobufSchemaParser.parse(mediumProtoSchema,
                "medium-message.proto");
        Descriptors.Descriptor messageDescriptor = fileDescriptor.findMessageTypeByName("MediumMessage");

        // Create nested profile message
        Descriptors.Descriptor profileDescriptor = messageDescriptor.findNestedTypeByName("Profile");
        DynamicMessage profile = DynamicMessage.newBuilder(profileDescriptor)
                .setField(profileDescriptor.findFieldByName("firstName"), "Jane")
                .setField(profileDescriptor.findFieldByName("lastName"), "Smith")
                .setField(profileDescriptor.findFieldByName("email"), "jane.smith@company.com")
                .setField(profileDescriptor.findFieldByName("phone"), "+1-555-0123")
                .build();

        return DynamicMessage.newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByName("userId"), "medium-user-67890")
                .setField(messageDescriptor.findFieldByName("profile"), profile)
                .setField(messageDescriptor.findFieldByName("age"), 30)
                .setField(messageDescriptor.findFieldByName("active"), true)
                .setField(messageDescriptor.findFieldByName("score"), 95.5)
                .setField(messageDescriptor.findFieldByName("loginCount"), 247L) // Use Long instead of Integer
                .setField(messageDescriptor.findFieldByName("accountType"), "premium")
                .setField(messageDescriptor.findFieldByName("createdAt"), "2023-01-15T10:30:00Z")
                .build();
    }

    private DynamicMessage createLargeProtobufMessage() throws Exception {
        String largeProtoSchema = loadProtobufSchema("large-message.proto");
        Descriptors.FileDescriptor fileDescriptor = ProtobufSchemaParser.parse(largeProtoSchema, "large-message.proto");
        Descriptors.Descriptor messageDescriptor = fileDescriptor.findMessageTypeByName("LargeMessage");

        // Create customer info
        Descriptors.Descriptor customerDescriptor = messageDescriptor.findNestedTypeByName("Customer");
        Descriptors.Descriptor addressDescriptor = customerDescriptor.findNestedTypeByName("Address");

        DynamicMessage billingAddress = DynamicMessage.newBuilder(addressDescriptor)
                .setField(addressDescriptor.findFieldByName("street"), "456 Enterprise Blvd")
                .setField(addressDescriptor.findFieldByName("city"), "New York")
                .setField(addressDescriptor.findFieldByName("state"), "NY")
                .setField(addressDescriptor.findFieldByName("zipCode"), "10001")
                .setField(addressDescriptor.findFieldByName("country"), "USA")
                .build();

        DynamicMessage customer = DynamicMessage.newBuilder(customerDescriptor)
                .setField(customerDescriptor.findFieldByName("customerId"), "large-cust-54321")
                .setField(customerDescriptor.findFieldByName("firstName"), "Robert")
                .setField(customerDescriptor.findFieldByName("lastName"), "Johnson")
                .setField(customerDescriptor.findFieldByName("email"), "robert.johnson@enterprise.com")
                .setField(customerDescriptor.findFieldByName("billingAddress"), billingAddress)
                .build();

        // Create order items
        Descriptors.Descriptor itemDescriptor = messageDescriptor.findNestedTypeByName("OrderItem");

        DynamicMessage item1 = DynamicMessage.newBuilder(itemDescriptor)
                .setField(itemDescriptor.findFieldByName("productId"), "prod-001")
                .setField(itemDescriptor.findFieldByName("name"), "Wireless Bluetooth Headphones")
                .setField(itemDescriptor.findFieldByName("category"), "Electronics")
                .setField(itemDescriptor.findFieldByName("price"), 199.99)
                .setField(itemDescriptor.findFieldByName("quantity"), 2)
                .build();

        DynamicMessage item2 = DynamicMessage.newBuilder(itemDescriptor)
                .setField(itemDescriptor.findFieldByName("productId"), "prod-002")
                .setField(itemDescriptor.findFieldByName("name"), "USB-C Charging Cable")
                .setField(itemDescriptor.findFieldByName("category"), "Accessories")
                .setField(itemDescriptor.findFieldByName("price"), 24.99)
                .setField(itemDescriptor.findFieldByName("quantity"), 3)
                .build();

        return DynamicMessage.newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByName("orderId"), "large-order-98765")
                .setField(messageDescriptor.findFieldByName("customer"), customer)
                .addRepeatedField(messageDescriptor.findFieldByName("items"), item1)
                .addRepeatedField(messageDescriptor.findFieldByName("items"), item2)
                .setField(messageDescriptor.findFieldByName("totalAmount"), 489.95)
                .setField(messageDescriptor.findFieldByName("currency"), "USD")
                .setField(messageDescriptor.findFieldByName("status"), "completed")
                .setField(messageDescriptor.findFieldByName("createdAt"), "2023-12-10T09:15:30Z")
                .build();
    }

    private String getProtobufSchemaFromMessage(DynamicMessage message, String sizeCategory) throws Exception {
        switch (sizeCategory) {
            case "SMALL":
                return loadProtobufSchema("small-message.proto");
            case "MEDIUM":
                return loadProtobufSchema("medium-message.proto");
            case "LARGE":
                return loadProtobufSchema("large-message.proto");
            default:
                throw new IllegalArgumentException("Unknown size category: " + sizeCategory);
        }
    }

}