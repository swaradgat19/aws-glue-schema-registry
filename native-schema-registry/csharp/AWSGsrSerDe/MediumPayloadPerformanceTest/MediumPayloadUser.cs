using System;

namespace MediumPayloadPerformanceTest
{
    /// <summary>
    /// Medium payload user record for performance testing - matches Java client structure
    /// </summary>
    public class MediumPayloadUser
    {
        public string UserId { get; set; } = string.Empty;
        public UserProfile Profile { get; set; } = new UserProfile();
        public UserAddress Address { get; set; } = new UserAddress();
        public UserPreferences Preferences { get; set; } = new UserPreferences();
        public UserMetadata Metadata { get; set; } = new UserMetadata();
    }
}
