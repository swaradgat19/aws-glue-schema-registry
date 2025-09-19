using System;

namespace MediumPayloadPerformanceTest
{
    /// <summary>
    /// User metadata for medium payload test
    /// </summary>
    public class UserMetadata
    {
        public string CreatedAt { get; set; } = string.Empty;
        public string LastLogin { get; set; } = string.Empty;
        public int LoginCount { get; set; }
        public string AccountType { get; set; } = string.Empty;
    }
}
