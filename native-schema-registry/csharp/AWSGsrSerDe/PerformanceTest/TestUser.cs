using System;

namespace PerformanceTest
{
    /// <summary>
    /// Small payload test record for performance testing - matches Java client structure
    /// </summary>
    public class TestUser
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public int Age { get; set; }
        public bool Active { get; set; }
    }
}
