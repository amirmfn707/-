package com.example.data.model

data class WpPost(
    val id: Long,
    val title: String,
    val content: String,
    val excerpt: String? = null,
    val status: String = "draft", // "draft", "publish", "pending"
    val dateGmt: String? = null,
    val link: String? = null
)

data class WcOrder(
    val id: Long,
    val number: String,
    val status: String, // "processing", "completed", "pending", "on-hold", "cancelled", "refunded"
    val total: String,
    val currency: String = "تومان",
    val customerName: String,
    val customerPhone: String? = null,
    val dateCreated: String,
    val itemsCount: Int = 1,
    val itemsSummary: String = "",
    val shippingAddress: String? = null
)

data class WcCoupon(
    val id: Long,
    val code: String,
    val amount: String,
    val discountType: String = "percent", // "percent", "fixed_cart", "fixed_product"
    val description: String? = null,
    val dateExpires: String? = null
)

data class WcSalesReport(
    val totalSales: String = "۰",
    val netSales: String = "۰",
    val averageSales: String = "۰",
    val totalOrders: Int = 0,
    val totalItems: Int = 0
)

data class WpConnectionStatus(
    val isConnected: Boolean,
    val siteName: String? = null,
    val userDisplayName: String? = null,
    val errorMessage: String? = null
)
