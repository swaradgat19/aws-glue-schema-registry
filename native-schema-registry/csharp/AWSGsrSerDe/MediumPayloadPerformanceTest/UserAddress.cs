using System;

namespace MediumPayloadPerformanceTest
{
    /// <summary>
    /// User address information for medium payload test
    /// </summary>
    public class UserAddress
    {
        public string Street { get; set; } = string.Empty;
        public string City { get; set; } = string.Empty;
        public string State { get; set; } = string.Empty;
        public string ZipCode { get; set; } = string.Empty;
        public string Country { get; set; } = string.Empty;
    }
}
