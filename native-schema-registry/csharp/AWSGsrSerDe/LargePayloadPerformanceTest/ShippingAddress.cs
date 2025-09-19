using System;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Shipping address for large payload test
    /// </summary>
    public class ShippingAddress
    {
        public string Street { get; set; } = string.Empty;
        public string Apartment { get; set; } = string.Empty;
        public string City { get; set; } = string.Empty;
        public string State { get; set; } = string.Empty;
        public string ZipCode { get; set; } = string.Empty;
        public string Country { get; set; } = string.Empty;
    }
}
