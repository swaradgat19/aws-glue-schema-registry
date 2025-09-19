using System;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Personal information for large payload test
    /// </summary>
    public class PersonalInfo
    {
        public string FirstName { get; set; } = string.Empty;
        public string LastName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Phone { get; set; } = string.Empty;
        public string DateOfBirth { get; set; } = string.Empty;
    }
}
