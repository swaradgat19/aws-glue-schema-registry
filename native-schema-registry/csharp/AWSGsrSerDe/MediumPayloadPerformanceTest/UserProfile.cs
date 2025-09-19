using System;

namespace MediumPayloadPerformanceTest
{
    /// <summary>
    /// User profile information for medium payload test
    /// </summary>
    public class UserProfile
    {
        public string FirstName { get; set; } = string.Empty;
        public string LastName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Phone { get; set; } = string.Empty;
        public string DateOfBirth { get; set; } = string.Empty;
    }
}
