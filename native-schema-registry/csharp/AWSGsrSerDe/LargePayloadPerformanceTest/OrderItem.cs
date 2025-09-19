using System;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Order item for large payload test
    /// </summary>
    public class OrderItem
    {
        public string ProductId { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Category { get; set; } = string.Empty;
        public double Price { get; set; }
        public int Quantity { get; set; }
        public ProductSpecifications Specifications { get; set; } = new ProductSpecifications();
    }
}
