using System;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Customer information for large payload test
    /// </summary>
    public class Customer
    {
        public string CustomerId { get; set; } = string.Empty;
        public PersonalInfo PersonalInfo { get; set; } = new PersonalInfo();
        public BillingAddress BillingAddress { get; set; } = new BillingAddress();
        public ShippingAddress ShippingAddress { get; set; } = new ShippingAddress();
    }
}
