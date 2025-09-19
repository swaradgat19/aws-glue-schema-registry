using System;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Card information for large payload test
    /// </summary>
    public class CardInfo
    {
        public string Last4 { get; set; } = string.Empty;
        public string Brand { get; set; } = string.Empty;
        public string ExpiryMonth { get; set; } = string.Empty;
        public string ExpiryYear { get; set; } = string.Empty;
    }
}
