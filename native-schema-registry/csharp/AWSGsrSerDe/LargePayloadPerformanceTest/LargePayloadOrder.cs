using System;
using System.Collections.Generic;

namespace LargePayloadPerformanceTest
{
    /// <summary>
    /// Large payload order record for performance testing - simplified structure
    /// </summary>
    public class LargePayloadOrder
    {
        // Order basic info
        public string OrderId { get; set; } = string.Empty;
        
        // Customer info (flattened)
        public string CustomerId { get; set; } = string.Empty;
        public string CustomerFirstName { get; set; } = string.Empty;
        public string CustomerLastName { get; set; } = string.Empty;
        public string CustomerEmail { get; set; } = string.Empty;
        public string CustomerPhone { get; set; } = string.Empty;
        public string CustomerDateOfBirth { get; set; } = string.Empty;
        
        // Billing address (flattened)
        public string BillingStreet { get; set; } = string.Empty;
        public string BillingSuite { get; set; } = string.Empty;
        public string BillingCity { get; set; } = string.Empty;
        public string BillingState { get; set; } = string.Empty;
        public string BillingZipCode { get; set; } = string.Empty;
        public string BillingCountry { get; set; } = string.Empty;
        
        // Shipping address (flattened)
        public string ShippingStreet { get; set; } = string.Empty;
        public string ShippingApartment { get; set; } = string.Empty;
        public string ShippingCity { get; set; } = string.Empty;
        public string ShippingState { get; set; } = string.Empty;
        public string ShippingZipCode { get; set; } = string.Empty;
        public string ShippingCountry { get; set; } = string.Empty;
        
        // Items (using existing OrderItem class)
        public List<OrderItem> Items { get; set; } = new List<OrderItem>();
        
        // Payment info (flattened)
        public string PaymentMethod { get; set; } = string.Empty;
        public string CardLast4 { get; set; } = string.Empty;
        public string CardBrand { get; set; } = string.Empty;
        public string CardExpiryMonth { get; set; } = string.Empty;
        public string CardExpiryYear { get; set; } = string.Empty;
        public string TransactionId { get; set; } = string.Empty;
        public double PaymentAmount { get; set; }
        public string PaymentCurrency { get; set; } = string.Empty;
        public string PaymentStatus { get; set; } = string.Empty;
        
        // Shipping info (flattened)
        public string ShippingMethod { get; set; } = string.Empty;
        public string ShippingCarrier { get; set; } = string.Empty;
        public string TrackingNumber { get; set; } = string.Empty;
        public string EstimatedDelivery { get; set; } = string.Empty;
        public double ShippingCost { get; set; }
        
        // Order metadata (flattened)
        public string CreatedAt { get; set; } = string.Empty;
        public string UpdatedAt { get; set; } = string.Empty;
        public string Source { get; set; } = string.Empty;
        public string SalesRep { get; set; } = string.Empty;
        public List<string> PromotionCodes { get; set; } = new List<string>();
        public string Notes { get; set; } = string.Empty;
    }
}
