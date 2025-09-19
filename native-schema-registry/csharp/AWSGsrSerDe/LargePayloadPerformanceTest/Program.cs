using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using AWSGsrSerDe.serializer;

namespace LargePayloadPerformanceTest
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("AWS Glue Schema Registry C# Serialization Performance Test - Large Payload");
            Console.WriteLine("============================================================================");
            
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
                // Create test object - matches Java client large payload
                var largeOrder = new LargePayloadOrder
                {
                    OrderId = "order-98765",
                    
                    // Customer info
                    CustomerId = "cust-54321",
                    CustomerFirstName = "Robert",
                    CustomerLastName = "Johnson",
                    CustomerEmail = "robert.johnson@enterprise.com",
                    CustomerPhone = "212-555-0123",
                    CustomerDateOfBirth = "1985-03-22",
                    
                    // Billing address
                    BillingStreet = "456 Enterprise Blvd",
                    BillingSuite = "Suite 789",
                    BillingCity = "New York",
                    BillingState = "NY",
                    BillingZipCode = "10001",
                    BillingCountry = "USA",
                    
                    // Shipping address
                    ShippingStreet = "123 Delivery Lane",
                    ShippingApartment = "Apt 4B",
                    ShippingCity = "Brooklyn",
                    ShippingState = "NY",
                    ShippingZipCode = "11201",
                    ShippingCountry = "USA",
                    
                    // Items
                    Items = new List<OrderItem>
                    {
                        new OrderItem
                        {
                            ProductId = "prod-001",
                            Name = "Wireless Bluetooth Headphones",
                            Category = "Electronics",
                            Price = 199.99,
                            Quantity = 2,
                            Specifications = new ProductSpecifications
                            {
                                Brand = "TechCorp",
                                Model = "WH-1000XM4",
                                Color = "Black",
                                Weight = "254g",
                                BatteryLife = "30 hours"
                            }
                        },
                        new OrderItem
                        {
                            ProductId = "prod-002",
                            Name = "USB-C Charging Cable",
                            Category = "Accessories",
                            Price = 24.99,
                            Quantity = 3,
                            Specifications = new ProductSpecifications
                            {
                                Brand = "CableCorp",
                                Length = "6 feet",
                                Color = "White",
                                DataTransfer = "480 Mbps",
                                PowerDelivery = "100W"
                            }
                        },
                        new OrderItem
                        {
                            ProductId = "prod-003",
                            Name = "Laptop Stand",
                            Category = "Office Supplies",
                            Price = 89.99,
                            Quantity = 1,
                            Specifications = new ProductSpecifications
                            {
                                Brand = "ErgoCorp",
                                Material = "Aluminum",
                                Adjustable = true,
                                MaxWeight = "15 lbs",
                                Dimensions = "12x10x6 inches"
                            }
                        }
                    },
                    
                    // Payment info
                    PaymentMethod = "credit_card",
                    CardLast4 = "4321",
                    CardBrand = "Visa",
                    CardExpiryMonth = "12",
                    CardExpiryYear = "2025",
                    TransactionId = "txn-abc123def456",
                    PaymentAmount = 489.95,
                    PaymentCurrency = "USD",
                    PaymentStatus = "completed",
                    
                    // Shipping info
                    ShippingMethod = "standard",
                    ShippingCarrier = "FedEx",
                    TrackingNumber = "1234567890123456",
                    EstimatedDelivery = "2023-12-15",
                    ShippingCost = 9.99,
                    
                    // Order metadata
                    CreatedAt = "2023-12-10T09:15:30Z",
                    UpdatedAt = "2023-12-10T09:16:45Z",
                    Source = "web",
                    SalesRep = "sales-rep-789",
                    PromotionCodes = new List<string> { "SAVE10", "FREESHIP" },
                    Notes = "Customer requested expedited processing"
                };

                RunSerializationBenchmark(largeOrder, iterations);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error: {ex.Message}");
                Environment.Exit(1);
            }
        }

        static void RunSerializationBenchmark(LargePayloadOrder largeOrder, int iterations)
        {
            var configPath = Path.Combine(AppContext.BaseDirectory, "large-payload-config.properties");
            
            // Initialize serializer
            var stopwatch = Stopwatch.StartNew();
            var serializer = new GlueSchemaRegistryKafkaSerializer(configPath);
            stopwatch.Stop();
            Console.WriteLine($"Serializer initialization: {stopwatch.ElapsedMilliseconds}ms");

            // First call (includes schema registration)
            stopwatch.Restart();
            var firstSerialized = serializer.Serialize(largeOrder, "large-payload-topic");
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
                serializer.Serialize(largeOrder, "large-payload-topic");
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
