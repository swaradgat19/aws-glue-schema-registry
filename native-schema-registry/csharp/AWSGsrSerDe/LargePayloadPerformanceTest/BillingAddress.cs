using System;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Billing address for large payload test
    /// </summary>
    public class BillingAddress
    {
        public string Street { get; set; } = string.Empty;
        public string Suite { get; set; } = string.Empty;
        public string City { get; set; } = string.Empty;
        public string State { get; set; } = string.Empty;
        public string ZipCode { get; set; } = string.Empty;
        public string Country { get; set; } = string.Empty;
    }
}
