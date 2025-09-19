using System;
using System.Diagnostics;
using System.IO;
using AWSGsrSerDe.serializer;

namespace MediumPayloadPerformanceTest
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("AWS Glue Schema Registry C# Serialization Performance Test - Medium Payload");
            Console.WriteLine("=============================================================================");
            
            // Parse iteration count from command line args
            int iterations = 50000; // default
            if (args.Length > 0 && int.TryParse(args[0], out int parsedIterations))
            {
                iterations = parsedIterations;
            }
            
            Console.WriteLine($"Testing with {iterations:N0} iterations");
            Console.WriteLine();
            
            try
            {
                // Create test object - matches Java client medium payload
                var mediumUser = new MediumPayloadUser
                {
                    UserId = "user-67890",
                    Profile = new UserProfile
                    {
                        FirstName = "Jane",
                        LastName = "Smith",
                        Email = "jane.smith@company.com",
                        Phone = "+1-555-0123",
                        DateOfBirth = "1990-05-15"
                    },
                    Address = new UserAddress
                    {
                        Street = "123 Main Street",
                        City = "San Francisco",
                        State = "CA",
                        ZipCode = "94105",
                        Country = "USA"
                    },
                    Preferences = new UserPreferences
                    {
                        Newsletter = true,
                        Notifications = false,
                        Theme = "dark",
                        Language = "en-US"
                    },
                    Metadata = new UserMetadata
                    {
                        CreatedAt = "2023-01-15T10:30:00Z",
                        LastLogin = "2023-12-01T14:22:33Z",
                        LoginCount = 247,
                        AccountType = "premium"
                    }
                };

                RunSerializationBenchmark(mediumUser, iterations);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error: {ex.Message}");
                Environment.Exit(1);
            }
        }

        static void RunSerializationBenchmark(MediumPayloadUser mediumUser, int iterations)
        {
            var configPath = Path.Combine(AppContext.BaseDirectory, "medium-payload-config.properties");
            
            // Initialize serializer
            var stopwatch = Stopwatch.StartNew();
            var serializer = new GlueSchemaRegistryKafkaSerializer(configPath);
            stopwatch.Stop();
            Console.WriteLine($"Serializer initialization: {stopwatch.ElapsedMilliseconds}ms");

            // First call (includes schema registration)
            stopwatch.Restart();
            var firstSerialized = serializer.Serialize(mediumUser, "medium-payload-topic");
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
                serializer.Serialize(mediumUser, "medium-payload-topic");
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
