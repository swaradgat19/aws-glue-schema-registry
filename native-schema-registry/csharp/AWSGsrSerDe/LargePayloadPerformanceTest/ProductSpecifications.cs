using System;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Product specifications for large payload test
    /// </summary>
    public class ProductSpecifications
    {
        public string Brand { get; set; } = string.Empty;
        public string Model { get; set; } = string.Empty;
        public string Color { get; set; } = string.Empty;
        public string Weight { get; set; } = string.Empty;
        public string BatteryLife { get; set; } = string.Empty;
        public string Length { get; set; } = string.Empty;
        public string DataTransfer { get; set; } = string.Empty;
        public string PowerDelivery { get; set; } = string.Empty;
        public string Material { get; set; } = string.Empty;
        public bool Adjustable { get; set; }
        public string MaxWeight { get; set; } = string.Empty;
        public string Dimensions { get; set; } = string.Empty;
    }
}
