using System;
using System.Diagnostics;
using System.IO;
using AWSGsrSerDe.serializer;

namespace PerformanceTest
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("AWS Glue Schema Registry C# Serialization Performance Test - Small Payload");
            Console.WriteLine("===========================================================================");
            
            // Parse iteration count from command line args
            int iterations = 10000; // default
            if (args.Length > 0 && int.TryParse(args[0], out int parsedIterations))
            {
                iterations = parsedIterations;
            }
            
            Console.WriteLine($"Testing with {iterations:N0} iterations");
            Console.WriteLine();
            
            try
            {
                // Create test object - matches Java client small payload
                var testUser = new TestUser
                {
                    Id = "user-12345",
                    Name = "John Doe",
                    Email = "john.doe@example.com",
                    Age = 30,
                    Active = true
                };

                RunSerializationBenchmark(testUser, iterations);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error: {ex.Message}");
                Environment.Exit(1);
            }
        }

        static void RunSerializationBenchmark(TestUser testUser, int iterations)
        {
            var configPath = Path.Combine(AppContext.BaseDirectory, "performance-test-config.properties");
            
            // Initialize serializer
            var stopwatch = Stopwatch.StartNew();
            var serializer = new GlueSchemaRegistryKafkaSerializer(configPath);
            stopwatch.Stop();
            Console.WriteLine($"Serializer initialization: {stopwatch.ElapsedMilliseconds}ms");

            // First call (includes schema registration)
            stopwatch.Restart();
            var firstSerialized = serializer.Serialize(testUser, "small-payload-topic");
            stopwatch.Stop();
            var firstCallTime = stopwatch.ElapsedMilliseconds;
            Console.WriteLine($"First call (with schema registration): {firstCallTime}ms");
            Console.WriteLine($"Message size: {firstSerialized?.Length ?? 0} bytes");
            Console.WriteLine();

            // Bulk serialization test
            Console.WriteLine($"Running {iterations:N0} cached serializations...");
            stopwatch.Restart();
            
            for (int i = 0; i < iterations; i++)
            {
                serializer.Serialize(testUser, "small-payload-topic");
            }
            
            stopwatch.Stop();
            var bulkTime = stopwatch.ElapsedMilliseconds;
            
            // Calculate metrics
            var avgTimePerMessage = (double)bulkTime / iterations;
            var throughput = iterations / (bulkTime / 1000.0);
            
            // Results
            Console.WriteLine("=== Results ===");
            Console.WriteLine($"Bulk serialization time: {bulkTime:N0}ms");
            Console.WriteLine($"Average per message: {avgTimePerMessage:F3}ms");
            Console.WriteLine($"Throughput: {throughput:N0} messages/second");
            Console.WriteLine($"Total messages processed: {iterations + 1:N0} (including first call)");
        }
    }
}
