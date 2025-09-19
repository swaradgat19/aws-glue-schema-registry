using System;

namespace MediumPayloadPerformanceTest
{
    /// <summary>
    /// User preferences for medium payload test
    /// </summary>
    public class UserPreferences
    {
        public bool Newsletter { get; set; }
        public bool Notifications { get; set; }
        public string Theme { get; set; } = string.Empty;
        public string Language { get; set; } = string.Empty;
    }
}
