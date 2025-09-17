package com.amazonaws.services.schemaregistry.integrationtests.performance;

import com.amazonaws.services.schemaregistry.common.Schema;
import com.amazonaws.services.schemaregistry.deserializers.json.JsonDeserializer;
import com.amazonaws.services.schemaregistry.serializers.json.JsonSerializer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.glue.model.DataFormat;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicJsonSerializationTest {

    @Test
    public void testBasicJsonSerializationDeserialization() throws Exception {
        // Test configuration
        final int WARMUP_ITERATIONS = 50;
        final int TEST_ITERATIONS = 500;
        
        // Simple JSON schema
        String jsonSchema = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"name\": {\"type\": \"string\"},\n" +
                "    \"age\": {\"type\": \"integer\"}\n" +
                "  },\n" +
                "  \"required\": [\"name\", \"age\"]\n" +
                "}";

        // Simple test object (as JSON string)
        String testData = "{\n" +
                "  \"name\": \"John Doe\",\n" +
                "  \"age\": 30\n" +
                "}";

        // Create JsonSerializer instance with minimal config
        JsonSerializer jsonSerializer = new JsonSerializer(null);

        System.out.println("JSON Serialization/Deserialization Performance Test");
        System.out.println("Warmup iterations: " + WARMUP_ITERATIONS);
        System.out.println("Test iterations: " + TEST_ITERATIONS);
        System.out.println("============================================================");

        // Warmup phase - JVM optimization
        System.out.println("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            byte[] serializedData = jsonSerializer.serialize(testData);
            String deserializedJson = new String(serializedData);
            // Verify basic functionality during warmup
            if (i == 0) {
                assertNotNull(serializedData);
                assertTrue(serializedData.length > 0);
                assertNotNull(deserializedJson);
                assertTrue(deserializedJson.contains("John Doe"));
            }
        }

        // Measurement phase
        System.out.println("Running performance measurements...");
        
        long[] serializationTimes = new long[TEST_ITERATIONS];
        long[] deserializationTimes = new long[TEST_ITERATIONS];
        int totalDataSize = 0;

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // Measure serialization time
            long serializeStartTime = System.nanoTime();
            byte[] serializedData = jsonSerializer.serialize(testData);
            long serializeEndTime = System.nanoTime();
            serializationTimes[i] = serializeEndTime - serializeStartTime;
            
            if (i == 0) {
                totalDataSize = serializedData.length;
            }

            // Measure deserialization time (simple JSON parsing)
            long deserializeStartTime = System.nanoTime();
            String deserializedJson = new String(serializedData);
            long deserializeEndTime = System.nanoTime();
            deserializationTimes[i] = deserializeEndTime - deserializeStartTime;
        }

        // Calculate statistics
        PerformanceStats serializationStats = calculateStats(serializationTimes);
        PerformanceStats deserializationStats = calculateStats(deserializationTimes);
        
        // Calculate total round-trip stats
        long[] totalTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            totalTimes[i] = serializationTimes[i] + deserializationTimes[i];
        }
        PerformanceStats totalStats = calculateStats(totalTimes);

        // Print results
        System.out.println("\n=== SERIALIZATION PERFORMANCE ===");
        printStats("Serialization", serializationStats);
        System.out.println("Serialized data size: " + totalDataSize + " bytes");
        
        System.out.println("\n=== DESERIALIZATION PERFORMANCE ===");
        printStats("Deserialization", deserializationStats);
        
        System.out.println("\n=== TOTAL ROUND-TRIP PERFORMANCE ===");
        printStats("Total Round-trip", totalStats);
        
        System.out.println("\n=== THROUGHPUT ANALYSIS ===");
        double serializationThroughput = 1_000_000_000.0 / serializationStats.mean;
        double deserializationThroughput = 1_000_000_000.0 / deserializationStats.mean;
        double totalThroughput = 1_000_000_000.0 / totalStats.mean;
        
        System.out.printf("Serialization throughput: %.2f operations/second%n", serializationThroughput);
        System.out.printf("Deserialization throughput: %.2f operations/second%n", deserializationThroughput);
        System.out.printf("Total round-trip throughput: %.2f operations/second%n", totalThroughput);
    }

    private PerformanceStats calculateStats(long[] times) {
        // Sort for percentile calculations
        long[] sortedTimes = times.clone();
        java.util.Arrays.sort(sortedTimes);
        
        // Calculate mean
        long sum = 0;
        for (long time : times) {
            sum += time;
        }
        double mean = (double) sum / times.length;
        
        // Calculate percentiles
        long min = sortedTimes[0];
        long max = sortedTimes[sortedTimes.length - 1];
        long median = sortedTimes[sortedTimes.length / 2];
        long p95 = sortedTimes[(int) (sortedTimes.length * 0.95)];
        long p99 = sortedTimes[(int) (sortedTimes.length * 0.99)];
        
        return new PerformanceStats(mean, min, max, median, p95, p99);
    }
    
    private void printStats(String operation, PerformanceStats stats) {
        System.out.printf("%s - Mean: %.3f ms (%.0f ns)%n", operation, stats.mean / 1_000_000.0, stats.mean);
        System.out.printf("%s - Min: %.3f ms (%.0f ns)%n", operation, stats.min / 1_000_000.0, (double) stats.min);
        System.out.printf("%s - Max: %.3f ms (%.0f ns)%n", operation, stats.max / 1_000_000.0, (double) stats.max);
        System.out.printf("%s - Median: %.3f ms (%.0f ns)%n", operation, stats.median / 1_000_000.0, (double) stats.median);
        System.out.printf("%s - 95th percentile: %.3f ms (%.0f ns)%n", operation, stats.p95 / 1_000_000.0, (double) stats.p95);
        System.out.printf("%s - 99th percentile: %.3f ms (%.0f ns)%n", operation, stats.p99 / 1_000_000.0, (double) stats.p99);
    }
    
    private static class PerformanceStats {
        final double mean;
        final long min;
        final long max;
        final long median;
        final long p95;
        final long p99;
        
        PerformanceStats(double mean, long min, long max, long median, long p95, long p99) {
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.median = median;
            this.p95 = p95;
            this.p99 = p99;
        }
    }
}