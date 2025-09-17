package com.amazonaws.services.schemaregistry.integrationtests.performance;

import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistryKafkaSerializer;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.glue.model.DataFormat;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import com.amazonaws.services.schemaregistry.deserializers.protobuf.ProtobufSchemaParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GSRKafkaSerializerPerformanceTest {

    private static final String REGION = "us-east-1";
    private static final String REGISTRY_NAME = "default-registry";
    private static final String TEST_TOPIC = "performance-test-topic";

    @BeforeAll
    static void setup() {
        System.out.println("=== KAFKA SERIALIZER PERFORMANCE TEST SETUP ===");
        System.out.println("✅ Setup complete");
    }

    // ===== JSON TESTS =====

    @Test
    public void testKafkaJsonSerializationPerformance() throws Exception {
        System.out.println("\n=== KAFKA JSON SERIALIZATION PERFORMANCE TEST ===");

        // Test different payload sizes
        testKafkaJsonPayloadSize("SMALL", createSmallJsonData(), "json-small-schema");
        testKafkaJsonPayloadSize("MEDIUM", createMediumJsonData(), "json-medium-schema");
        testKafkaJsonPayloadSize("LARGE", createLargeJsonData(), "json-large-schema");

        System.out.println("\n✅ All Kafka JSON serialization performance tests completed!");
    }

    @Test
    public void testKafkaAvroSerializationPerformance() throws Exception {
        System.out.println("\n=== KAFKA AVRO SERIALIZATION PERFORMANCE TEST ===");

        // Test different payload sizes
        testKafkaAvroPayloadSize("SMALL", createSmallAvroData(), "avro-small-schema");
        testKafkaAvroPayloadSize("MEDIUM", createMediumAvroData(), "avro-medium-schema");
        testKafkaAvroPayloadSize("LARGE", createLargeAvroData(), "avro-large-schema");

        System.out.println("\n✅ All Kafka Avro serialization performance tests completed!");
    }

    @Test
    public void testKafkaProtobufSerializationPerformance() throws Exception {
        System.out.println("\n=== KAFKA PROTOBUF SERIALIZATION PERFORMANCE TEST ===");

        // Test different payload sizes
        testKafkaProtobufPayloadSize("SMALL", createSmallProtobufMessage(), "protobuf-small-schema");
        testKafkaProtobufPayloadSize("MEDIUM", createMediumProtobufMessage(), "protobuf-medium-schema");
        testKafkaProtobufPayloadSize("LARGE", createLargeProtobufMessage(), "protobuf-large-schema");

        System.out.println("\n✅ All Kafka Protobuf serialization performance tests completed!");
    }

    // ===== KAFKA SERIALIZER TEST METHODS (TO BE IMPLEMENTED) =====

    private void testKafkaJsonPayloadSize(String sizeCategory, Object jsonData, String schemaName) throws Exception {
        System.out.println("\n=== " + sizeCategory + " KAFKA JSON PAYLOAD TEST ===");

        // Create Kafka serializer with JSON configuration
        Map<String, Object> configs = createKafkaConfigs(DataFormat.JSON, schemaName);
        GlueSchemaRegistryKafkaSerializer serializer = createKafkaSerializer(configs);

        // Calculate payload size
        String jsonString = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(jsonData);
        System.out.println("Payload size: " + jsonString.length() + " characters");
        System.out.println(
                "Sample data: " + (jsonString.length() > 100 ? jsonString.substring(0, 100) + "..." : jsonString));

        // === WARMUP PHASE ===
        System.out.println("\n--- Warmup (10 iterations) ---");
        for (int i = 0; i < 10; i++) {
            byte[] serialized = serializer.serialize(TEST_TOPIC, jsonData);
            assertNotNull(serialized);
        }
        System.out.println("✅ Warmup complete");

        // === MEASUREMENT PHASE ===
        System.out.println("\n--- Measurement (50,000 iterations) ---");

        long startTime = System.nanoTime();

        for (int i = 0; i < 50000; i++) {
            serializer.serialize(TEST_TOPIC, jsonData);
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        // === STATISTICS ===
        double avgMs = (totalTime / 50000.0) / 1_000_000.0;

        System.out.println("\n--- " + sizeCategory + " KAFKA JSON RESULTS ---");
        System.out.println("Payload Size: " + jsonString.length() + " characters");
        System.out.println("Iterations: 50,000");
        System.out.println("Average: " + String.format("%.3f", avgMs) + " ms");

        // Calculate throughput
        double totalTimeSeconds = totalTime / 1_000_000_000.0;
        double throughputPerSecond = 50000 / totalTimeSeconds;

        System.out.println("\n--- " + sizeCategory + " KAFKA THROUGHPUT ---");
        System.out.println("Total time: " + String.format("%.3f", totalTimeSeconds) + " seconds");
        System.out.println("Throughput: " + String.format("%.0f", throughputPerSecond) + " serializations/second");
        System.out.println("Bytes per second: " + String.format("%.0f", throughputPerSecond * jsonString.length())
                + " chars/second");

        System.out.println("\n✅ " + sizeCategory + " Kafka JSON test completed!");

        // Clean up
        serializer.close();
    }

    private void testKafkaAvroPayloadSize(String sizeCategory, GenericRecord avroData, String schemaName)
            throws Exception {
        System.out.println("\n=== " + sizeCategory + " KAFKA AVRO PAYLOAD TEST ===");

        // Create Kafka serializer with Avro configuration
        Map<String, Object> configs = createKafkaConfigs(DataFormat.AVRO, schemaName);
        GlueSchemaRegistryKafkaSerializer serializer = createKafkaSerializer(configs);

        // Calculate record size
        byte[] recordBytes = avroData.toString().getBytes();
        System.out.println("Record size: " + recordBytes.length + " bytes");
        System.out.println("Sample record: "
                + avroData.toString().substring(0, Math.min(100, avroData.toString().length())) + "...");

        // === WARMUP PHASE ===
        System.out.println("\n--- Warmup (10 iterations) ---");
        for (int i = 0; i < 10; i++) {
            byte[] serialized = serializer.serialize(TEST_TOPIC, avroData);
            assertNotNull(serialized);
        }
        System.out.println("✅ Warmup complete");

        // === MEASUREMENT PHASE ===
        System.out.println("\n--- Measurement (50,000 iterations) ---");

        long startTime = System.nanoTime();

        for (int i = 0; i < 50000; i++) {
            serializer.serialize(TEST_TOPIC, avroData);
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        // === STATISTICS ===
        double avgMs = (totalTime / 50000.0) / 1_000_000.0;

        System.out.println("\n--- " + sizeCategory + " KAFKA AVRO RESULTS ---");
        System.out.println("Record Size: " + recordBytes.length + " bytes");
        System.out.println("Iterations: 50,000");
        System.out.println("Average: " + String.format("%.3f", avgMs) + " ms");

        // Calculate throughput
        double totalTimeSeconds = totalTime / 1_000_000_000.0;
        double throughputPerSecond = 50000 / totalTimeSeconds;

        System.out.println("\n--- " + sizeCategory + " KAFKA AVRO THROUGHPUT ---");
        System.out.println("Total time: " + String.format("%.3f", totalTimeSeconds) + " seconds");
        System.out.println("Throughput: " + String.format("%.0f", throughputPerSecond) + " serializations/second");
        System.out.println("Bytes per second: " + String.format("%.0f", throughputPerSecond * recordBytes.length)
                + " bytes/second");

        System.out.println("✅ " + sizeCategory + " Kafka Avro test completed!");

        // Clean up
        serializer.close();
    }

    private void testKafkaProtobufPayloadSize(String sizeCategory, DynamicMessage protobufData, String schemaName)
            throws Exception {
        System.out.println("\n=== " + sizeCategory + " KAFKA PROTOBUF PAYLOAD TEST ===");

        // Create Kafka serializer with Protobuf configuration
        Map<String, Object> configs = createKafkaConfigs(DataFormat.PROTOBUF, schemaName);
        GlueSchemaRegistryKafkaSerializer serializer = createKafkaSerializer(configs);

        // Calculate message size
        byte[] messageBytes = protobufData.toByteArray();
        System.out.println("Message size: " + messageBytes.length + " bytes");
        System.out.println("Sample message: "
                + protobufData.toString().substring(0, Math.min(100, protobufData.toString().length())) + "...");

        // === WARMUP PHASE ===
        System.out.println("\n--- Warmup (10 iterations) ---");
        for (int i = 0; i < 10; i++) {
            byte[] serialized = serializer.serialize(TEST_TOPIC, protobufData);
            assertNotNull(serialized);
        }
        System.out.println("✅ Warmup complete");

        // === MEASUREMENT PHASE ===
        System.out.println("\n--- Measurement (50,000 iterations) ---");

        long startTime = System.nanoTime();

        for (int i = 0; i < 50000; i++) {
            serializer.serialize(TEST_TOPIC, protobufData);
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        // === STATISTICS ===
        double avgMs = (totalTime / 50000.0) / 1_000_000.0;

        System.out.println("\n--- " + sizeCategory + " KAFKA PROTOBUF RESULTS ---");
        System.out.println("Message Size: " + messageBytes.length + " bytes");
        System.out.println("Iterations: 50,000");
        System.out.println("Average: " + String.format("%.3f", avgMs) + " ms");

        // Calculate throughput
        double totalTimeSeconds = totalTime / 1_000_000_000.0;
        double throughputPerSecond = 50000 / totalTimeSeconds;

        System.out.println("\n--- " + sizeCategory + " KAFKA PROTOBUF THROUGHPUT ---");
        System.out.println("Total time: " + String.format("%.3f", totalTimeSeconds) + " seconds");
        System.out.println("Throughput: " + String.format("%.0f", throughputPerSecond) + " serializations/second");
        System.out.println("Bytes per second: " + String.format("%.0f", throughputPerSecond * messageBytes.length)
                + " bytes/second");

        System.out.println("\n✅ " + sizeCategory + " Kafka Protobuf test completed!");

        // Clean up
        serializer.close();
    }

    // ===== HELPER METHODS FOR KAFKA SERIALIZER CONFIGURATION =====

    private Map<String, Object> createKafkaConfigs(DataFormat dataFormat, String schemaName) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AWSSchemaRegistryConstants.AWS_REGION, REGION);
        configs.put(AWSSchemaRegistryConstants.REGISTRY_NAME, REGISTRY_NAME);
        configs.put(AWSSchemaRegistryConstants.SCHEMA_NAME, schemaName);
        configs.put(AWSSchemaRegistryConstants.DATA_FORMAT, dataFormat.name());
        configs.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, true);
        return configs;
    }

    private GlueSchemaRegistryKafkaSerializer createKafkaSerializer(Map<String, Object> configs) {
        GlueSchemaRegistryKafkaSerializer serializer = new GlueSchemaRegistryKafkaSerializer(
                DefaultCredentialsProvider.builder().build(), configs);
        serializer.configure(configs, false); // false = value serializer
        return serializer;
    }

    // ===== DATA CREATION METHODS (REUSE FROM EXISTING TEST) =====

    // JSON Data Creation
    private Object createSmallJsonData() throws IOException {
        String jsonData = loadJsonFile("small-payload.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(jsonData, Object.class);
    }

    private Object createMediumJsonData() throws IOException {
        String jsonData = loadJsonFile("medium-payload.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(jsonData, Object.class);
    }

    private Object createLargeJsonData() throws IOException {
        String jsonData = loadJsonFile("large-payload.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readValue(jsonData, Object.class);
    }

    // Avro Data Creation
    private GenericRecord createSmallAvroData() throws Exception {
        Schema schema = new Schema.Parser()
                .parse(new File("src/test/resources/performance/avro/small-schema.avsc"));
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", "small-kafka-avro-12345");
        record.put("message", "Small Kafka Avro performance test message");
        record.put("number", 42);
        return record;
    }

    private GenericRecord createMediumAvroData() throws Exception {
        Schema schema = new Schema.Parser()
                .parse(new File("src/test/resources/performance/avro/medium-schema.avsc"));
        GenericRecord record = new GenericData.Record(schema);

        // Create nested profile record
        Schema profileSchema = schema.getField("profile").schema();
        GenericRecord profile = new GenericData.Record(profileSchema);
        profile.put("firstName", "Jane");
        profile.put("lastName", "Smith");
        profile.put("email", "jane.smith@kafka-test.com");
        profile.put("phone", "+1-555-0123");

        record.put("userId", "medium-kafka-avro-67890");
        record.put("profile", profile);
        record.put("age", 30);
        record.put("active", true);
        record.put("score", 95.5);
        record.put("loginCount", 247L);
        record.put("accountType", "premium");
        record.put("createdAt", "2023-01-15T10:30:00Z");

        return record;
    }

    private GenericRecord createLargeAvroData() throws Exception {
        Schema schema = new Schema.Parser()
                .parse(new File("src/test/resources/performance/avro/large-schema.avsc"));
        GenericRecord record = new GenericData.Record(schema);

        // Create customer with address
        Schema customerSchema = schema.getField("customer").schema();
        GenericRecord customer = new GenericData.Record(customerSchema);

        Schema addressSchema = customerSchema.getField("billingAddress").schema();
        GenericRecord address = new GenericData.Record(addressSchema);
        address.put("street", "456 Kafka Enterprise Blvd");
        address.put("city", "New York");
        address.put("state", "NY");
        address.put("zipCode", "10001");
        address.put("country", "USA");

        customer.put("customerId", "large-kafka-avro-54321");
        customer.put("firstName", "Robert");
        customer.put("lastName", "Johnson");
        customer.put("email", "robert.johnson@kafka-enterprise.com");
        customer.put("billingAddress", address);

        // Create order items array
        Schema itemsSchema = schema.getField("items").schema();
        Schema itemSchema = itemsSchema.getElementType();

        GenericRecord item1 = new GenericData.Record(itemSchema);
        item1.put("productId", "kafka-prod-001");
        item1.put("name", "Kafka Wireless Bluetooth Headphones");
        item1.put("category", "Electronics");
        item1.put("price", 199.99);
        item1.put("quantity", 2);

        GenericRecord item2 = new GenericData.Record(itemSchema);
        item2.put("productId", "kafka-prod-002");
        item2.put("name", "Kafka USB-C Charging Cable");
        item2.put("category", "Accessories");
        item2.put("price", 24.99);
        item2.put("quantity", 3);

        java.util.List<GenericRecord> items = java.util.Arrays.asList(item1, item2);

        record.put("orderId", "large-kafka-avro-98765");
        record.put("customer", customer);
        record.put("items", items);
        record.put("totalAmount", 489.95);
        record.put("currency", "USD");
        record.put("status", "completed");
        record.put("createdAt", "2023-12-10T09:15:30Z");

        return record;
    }

    // Protobuf Data Creation
    private DynamicMessage createSmallProtobufMessage() throws Exception {
        String protoSchema = loadProtobufSchema("small-message.proto");
        Descriptors.FileDescriptor fileDescriptor = ProtobufSchemaParser.parse(protoSchema, "small-message.proto");
        Descriptors.Descriptor messageDescriptor = fileDescriptor.findMessageTypeByName("SimpleMessage");

        return DynamicMessage.newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByName("id"), "small-kafka-protobuf-12345")
                .setField(messageDescriptor.findFieldByName("message"), "Small Kafka Protobuf performance test message")
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
                .setField(profileDescriptor.findFieldByName("email"), "jane.smith@kafka-company.com")
                .setField(profileDescriptor.findFieldByName("phone"), "+1-555-0123")
                .build();

        return DynamicMessage.newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByName("userId"), "medium-kafka-protobuf-67890")
                .setField(messageDescriptor.findFieldByName("profile"), profile)
                .setField(messageDescriptor.findFieldByName("age"), 30)
                .setField(messageDescriptor.findFieldByName("active"), true)
                .setField(messageDescriptor.findFieldByName("score"), 95.5)
                .setField(messageDescriptor.findFieldByName("loginCount"), 247L)
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
                .setField(addressDescriptor.findFieldByName("street"), "456 Kafka Enterprise Blvd")
                .setField(addressDescriptor.findFieldByName("city"), "New York")
                .setField(addressDescriptor.findFieldByName("state"), "NY")
                .setField(addressDescriptor.findFieldByName("zipCode"), "10001")
                .setField(addressDescriptor.findFieldByName("country"), "USA")
                .build();

        DynamicMessage customer = DynamicMessage.newBuilder(customerDescriptor)
                .setField(customerDescriptor.findFieldByName("customerId"), "large-kafka-protobuf-54321")
                .setField(customerDescriptor.findFieldByName("firstName"), "Robert")
                .setField(customerDescriptor.findFieldByName("lastName"), "Johnson")
                .setField(customerDescriptor.findFieldByName("email"), "robert.johnson@kafka-enterprise.com")
                .setField(customerDescriptor.findFieldByName("billingAddress"), billingAddress)
                .build();

        // Create order items
        Descriptors.Descriptor itemDescriptor = messageDescriptor.findNestedTypeByName("OrderItem");

        DynamicMessage item1 = DynamicMessage.newBuilder(itemDescriptor)
                .setField(itemDescriptor.findFieldByName("productId"), "kafka-prod-001")
                .setField(itemDescriptor.findFieldByName("name"), "Kafka Wireless Bluetooth Headphones")
                .setField(itemDescriptor.findFieldByName("category"), "Electronics")
                .setField(itemDescriptor.findFieldByName("price"), 199.99)
                .setField(itemDescriptor.findFieldByName("quantity"), 2)
                .build();

        DynamicMessage item2 = DynamicMessage.newBuilder(itemDescriptor)
                .setField(itemDescriptor.findFieldByName("productId"), "kafka-prod-002")
                .setField(itemDescriptor.findFieldByName("name"), "Kafka USB-C Charging Cable")
                .setField(itemDescriptor.findFieldByName("category"), "Accessories")
                .setField(itemDescriptor.findFieldByName("price"), 24.99)
                .setField(itemDescriptor.findFieldByName("quantity"), 3)
                .build();

        return DynamicMessage.newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByName("orderId"), "large-kafka-protobuf-98765")
                .setField(messageDescriptor.findFieldByName("customer"), customer)
                .addRepeatedField(messageDescriptor.findFieldByName("items"), item1)
                .addRepeatedField(messageDescriptor.findFieldByName("items"), item2)
                .setField(messageDescriptor.findFieldByName("totalAmount"), 489.95)
                .setField(messageDescriptor.findFieldByName("currency"), "USD")
                .setField(messageDescriptor.findFieldByName("status"), "completed")
                .setField(messageDescriptor.findFieldByName("createdAt"), "2023-12-10T09:15:30Z")
                .build();
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

    private double calculateStandardDeviation(long[] values, double meanNanos) {
        double sumSquaredDiffs = 0.0;
        for (long value : values) {
            double diff = value - meanNanos;
            sumSquaredDiffs += diff * diff;
        }
        double variance = sumSquaredDiffs / values.length;
        return Math.sqrt(variance) / 1_000_000.0; // Convert to milliseconds
    }

}